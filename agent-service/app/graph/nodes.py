import os
from app.github_client import (
    get_repository_commit_sha,
    get_repository_file,
)
from app.graph.patch_validator import validate_patch
from app.ingestion.repository import index_repository
from langchain_ollama import ChatOllama
from app.graph.state import AgentState
from app.graph.retrieval import select_relevant_files
from app.ingestion.search import search_similar_chunks

llm = ChatOllama(
    model=os.getenv("OLLAMA_MODEL", "qwen2.5-coder:7b"),
    base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434"),
    temperature=0,
)


def ask_model(prompt: str) -> str:
    response = llm.invoke(prompt)
    return str(response.content).strip()


def plan_investigation(state: AgentState) -> dict:
    prompt = f"""
You are a senior software engineer investigating a bug.

Repository: {state["repository"]}
Branch: {state["branch"]}
Issue: {state["issue"]}

Create a concise investigation plan.

Identify:
1. What behavior needs investigation.
2. What source code and tests would provide evidence.
3. What must be verified before proposing a fix.

Do not claim to have inspected files or run tests.
Return only the plan.
"""
    return {"plan": ask_model(prompt)}


def retrieve_context(state: AgentState) -> dict:
    supplied_context = state.get("supplied_context", "").strip()

    # Prefer context explicitly supplied by the user.
    if supplied_context:
        return {"retrieved_context": supplied_context}

    repository = state.get("repository", "")
    branch = state.get("branch", "main")
    issue = state.get("issue", "")
    file_path = state.get("file_path", "")
    use_github = state.get("use_github", False)

    if not use_github or not repository:
        return {"retrieved_context": ""}

    try:
        # If a specific file was requested, retrieve it directly.
        if file_path:
            source = get_repository_file(
                repository=repository,
                file_path=file_path,
                ref=branch,
            )

            return {
                "retrieved_context": (
                    f"File: {file_path}\n"
                    f"```\n{source}\n```"
                )
            }
        # Otherwise, attempt semantic retrieval of relevant code chunks.
        commit_sha = get_repository_commit_sha(
            repository=repository,
            ref=branch,
        )

        # Ensure the current repository commit is indexed before semantic search.
        index_repository(
            repository=repository,
            issue=issue,
            ref=branch,
        )

        results = search_similar_chunks(
            query=issue,
            repository=repository,
            commit_sha=commit_sha,
            limit=5,
        )

        if results:
            file_contents = []

            for result in results:
                file_contents.append(
                    f"File: {result['file_path']}\n"
                    f"Lines: {result['start_line']}-{result['end_line']}\n"
                    f"Similarity: {result['similarity']:.3f}\n"
                    f"```\n{result['content']}\n```"
                )

            return {
                "retrieved_context": "\n\n".join(file_contents)
            }

        # Fall back to the existing heuristic retrieval if semantic
        # retrieval has no indexed chunks yet.
        selected_files = select_relevant_files(
            repository=repository,
            issue=issue,
            ref=branch,
            limit=5,
        )

        if not selected_files:
            return {
                "retrieved_context": "",
                "retrieval_error": (
                    "Insufficient evidence: no relevant source files "
                    "or indexed code chunks were found."
                ),
            }

        file_contents = []

        for path in selected_files:
            source = get_repository_file(
                repository=repository,
                file_path=path,
                ref=branch,
            )

            file_contents.append(
                f"File: {path}\n"
                f"```\n{source}\n```"
            )

        return {
            "retrieved_context": "\n\n".join(file_contents)
        }

    except Exception as exc:
        return {
            "retrieved_context": "",
            "retrieval_error": (
                f"Repository retrieval failed: "
                f"{type(exc).__name__}: {exc}"
            ),
        }

def diagnose_issue(state: AgentState) -> dict:
    retrieval_error = state.get("retrieval_error", "").strip()

    if retrieval_error:
        return {
            "diagnosis": retrieval_error
        }
    context = state.get("retrieved_context", "").strip()

    if not context:
        return {
            "diagnosis": (
                "Insufficient evidence: no source code was provided. "
                "Repository retrieval is not implemented in Phase 1."
            )
        }

    prompt = f"""
        You are a senior software engineer performing evidence-based debugging.

        Issue:
        {state["issue"]}

        Repository:
        {state["repository"]}

        Branch:
        {state["branch"]}

        Investigation plan:
        {state["plan"]}

        Source evidence:
        {context}

        Follow these rules strictly:
        1. First inspect the supplied code for explicit bugs.
        2. Identify what the code demonstrably does, not what you assume it does.
        3. If an argument is hardcoded incorrectly, identify that as the direct cause.
        4. Do not blame callers, configuration, or external code without evidence.
        5. Separate confirmed observations from unverified hypotheses.
        6. Do not invent file paths, line numbers, or test results.
        7. If the evidence is insufficient, explicitly state what is missing.

        Return these sections:

        ROOT CAUSE:
        State the confirmed root cause when the supplied code directly demonstrates it.
        Use "Likely cause" only when the evidence supports a strong hypothesis but does not prove it.
        If the evidence is insufficient, say so explicitly.

        DIRECT EVIDENCE:
        Quote the relevant supplied code and explain why it is problematic.

        HYPOTHESES:
        List any unverified assumptions, or state "None".

        REMAINING UNCERTAINTIES:
        Explain what cannot be determined from the supplied evidence.
        """
    return {"diagnosis": ask_model(prompt)}


def propose_patch(state: AgentState) -> dict:
    context = state.get("retrieved_context", "").strip()
    diagnosis = state.get("diagnosis", "")
    file_path = state.get("file_path", "").strip()

    if not context:
        return {
            "proposed_patch": (
                "No patch proposed because no source evidence is available."
            )
        }

    if not file_path:
        return {
            "proposed_patch": (
                "No patch proposed because the source file path is unknown."
            )
        }

    diagnosis_lower = diagnosis.lower()
    insufficient_evidence_phrases = (
        "insufficient evidence",
        "evidence is insufficient",
        "cannot determine the root cause",
        "not enough evidence",
        "cannot be determined from the supplied evidence",
        "does not explicitly contain any bugs",
        "no confirmed root cause",
        "no concrete bug",
        "no demonstrated defect",
    )

    if any(
        phrase in diagnosis_lower
        for phrase in insufficient_evidence_phrases
    ):
        return {
            "proposed_patch": (
                "No patch proposed because the diagnosis indicates "
                "insufficient evidence."
            )
        }

    prompt = f"""
    You are a senior software engineer proposing a minimal bug fix.

    Issue:
    {state["issue"]}

    Diagnosis:
    {diagnosis}

    Supplied source code:
    {context}

    Return a unified diff showing the proposed changes.

    Requirements:
    - Use standard unified diff format.
    - Include --- and +++ file headers.
    - Include an @@ hunk header.
    - Prefix removed lines with - and added lines with +.
    - Include unchanged context lines with a leading space when appropriate.
    - Change only code supported by the supplied evidence.
    - Use only the provided file path. Never invent or substitute a file path.
    - Do not claim the patch has been applied or tested.
    - Do not include Markdown code fences.
    - If a safe fix cannot be determined, explain why instead of inventing one.

    Return only the diff or a brief explanation.
    """

    proposed_patch = ask_model(prompt)

    validation = validate_patch(
        patch=proposed_patch,
        expected_file_path=file_path,
    )

    if not validation.valid:
        return {
            "proposed_patch": (
                "No patch proposed because the generated patch failed "
                f"validation: {validation.reason}"
            )
        }

    return {"proposed_patch": proposed_patch}

def verify_patch(state: AgentState) -> dict:
    return {
        "verification": (
            "NOT_RUN: Phase 1 does not execute tests or apply patches. "
            "Independent verification is required."
        ),
        "status": "completed_unverified",
    }