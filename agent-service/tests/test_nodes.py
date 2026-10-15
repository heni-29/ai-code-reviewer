from unittest.mock import patch

from app.graph.nodes import diagnose_issue, propose_patch


def test_diagnose_issue_without_context():
    state = {
        "repository": "demo/example",
        "branch": "main",
        "issue": "User lookup returns the wrong user.",
        "supplied_context": "",
        "retrieved_context": "",
    }

    result = diagnose_issue(state)

    assert "Insufficient evidence" in result["diagnosis"]


def test_diagnose_issue_uses_model():
    state = {
        "repository": "demo/example",
        "branch": "main",
        "issue": "The method passes null to the repository.",
        "plan": "Inspect the repository call.",
        "retrieved_context": (
            "public User getUser(Long id) {\n"
            "    return repository.findUser(null);\n"
            "}"
        ),
    }

    expected = (
        "ROOT CAUSE: The method passes null instead of id."
    )

    with patch(
        "app.graph.nodes.ask_model",
        return_value=expected,
    ) as mock_model:
        result = diagnose_issue(state)

    assert result["diagnosis"] == expected
    mock_model.assert_called_once()


def test_propose_patch_without_context():
    state = {
        "issue": "Fix the user lookup.",
        "diagnosis": "The method passes null.",
        "retrieved_context": "",
    }

    result = propose_patch(state)

    assert "No patch proposed" in result["proposed_patch"]


def test_propose_patch_uses_model():
    state = {
        "issue": "Fix the user lookup.",
        "diagnosis": "The method passes null instead of id.",
        "retrieved_context": (
            "public User getUser(Long id) {\n"
            "    return repository.findUser(null);\n"
            "}"
        ),
    }

    expected = (
        "--- a/UnknownFile.java\n"
        "+++ b/UnknownFile.java\n"
        "@@ -1,3 +1,3 @@\n"
        " public User getUser(Long id) {\n"
        "-    return repository.findUser(null);\n"
        "+    return repository.findUser(id);\n"
        " }"
    )

    with patch(
        "app.graph.nodes.ask_model",
        return_value=expected,
    ) as mock_model:
        result = propose_patch(state)

    assert result["proposed_patch"] == expected
    mock_model.assert_called_once()

def test_propose_patch_rejects_insufficient_evidence():
    state = {
        "issue": "Review this README for bugs.",
        "diagnosis": (
            "The provided evidence is insufficient to determine "
            "the root cause of any issues."
        ),
        "retrieved_context": "Hello World!",
    }

    result = propose_patch(state)

    assert "No patch proposed" in result["proposed_patch"]

def test_propose_patch_rejects_unconfirmed_bug():
    state = {
        "issue": "Investigate potential problems in the owner service.",
        "diagnosis": (
            "The code does not explicitly contain any bugs. "
            "No confirmed root cause has been established."
        ),
        "retrieved_context": (
            "public class OwnerService {\n"
            "    public Owner findOwner(Integer id) {\n"
            "        return repository.findById(id).orElse(null);\n"
            "    }\n"
            "}"
        ),
    }

    result = propose_patch(state)

    assert "No patch proposed" in result["proposed_patch"]

def test_diagnose_issue_preserves_retrieval_error():
    state = {
        "repository": "octocat/Hello-World",
        "branch": "master",
        "issue": "Review the project",
        "plan": "Inspect the relevant source files.",
        "retrieved_context": "",
        "retrieval_error": (
            "Repository retrieval failed: RuntimeError: GitHub unavailable"
        ),
    }

    with patch("app.graph.nodes.ask_model") as mock_ask_model:
        result = diagnose_issue(state)

    assert result["diagnosis"] == state["retrieval_error"]
    mock_ask_model.assert_not_called()
