from typing import TypedDict


class AgentState(TypedDict, total=False):
    repository: str
    issue: str
    branch: str
    supplied_context: str
    plan: str
    retrieved_context: str
    diagnosis: str
    proposed_patch: str
    verification: str
    status: str
    file_path: str
    use_github: bool