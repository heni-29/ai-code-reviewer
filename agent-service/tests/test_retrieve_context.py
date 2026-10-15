from unittest.mock import patch

from app.graph.nodes import retrieve_context


def test_retrieve_context_prefers_supplied_context():
    state = {
        "supplied_context": "public class UserService {}",
        "repository": "demo/example",
        "use_github": True,
    }

    result = retrieve_context(state)

    assert "public class UserService {}" in result["retrieved_context"]


def test_retrieve_context_fetches_specific_file():
    state = {
        "repository": "demo/example",
        "branch": "main",
        "issue": "Fix user lookup",
        "file_path": "src/UserService.java",
        "use_github": True,
        "supplied_context": "",
    }

    with patch(
        "app.graph.nodes.get_repository_file",
        return_value="public class UserService {}",
    ):
        result = retrieve_context(state)

    assert "src/UserService.java" in result["retrieved_context"]
    assert "public class UserService {}" in result["retrieved_context"]


def test_retrieve_context_selects_multiple_files():
    state = {
        "repository": "demo/example",
        "branch": "main",
        "issue": "Fix user lookup",
        "use_github": True,
        "supplied_context": "",
    }

    with (
        patch(
            "app.graph.nodes.select_relevant_files",
            return_value=[
                "src/UserService.java",
                "src/UserRepository.java",
            ],
        ),
        patch(
            "app.graph.nodes.get_repository_file",
            side_effect=[
                "public class UserService {}",
                "public interface UserRepository {}",
            ],
        ),
    ):
        result = retrieve_context(state)

    assert "src/UserService.java" in result["retrieved_context"]
    assert "src/UserRepository.java" in result["retrieved_context"]


def test_retrieve_context_handles_no_relevant_files():
    state = {
        "repository": "octocat/Hello-World",
        "branch": "master",
        "issue": "Review the project",
        "use_github": True,
        "supplied_context": "",
    }

    with patch(
        "app.graph.nodes.select_relevant_files",
        return_value=[],
    ):
        result = retrieve_context(state)

    assert result["retrieved_context"] == ""
    assert "Insufficient evidence" in result["diagnosis"]