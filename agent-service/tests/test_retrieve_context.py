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
        patch(
            "app.graph.nodes.get_repository_commit_sha",
            return_value="abc123",
        ),
        patch(
            "app.graph.nodes.search_similar_chunks",
            return_value=[],
        ),
        patch(
            "app.graph.nodes.index_repository",
            return_value=("abc123", 0),
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
    assert "Insufficient evidence" in result["retrieval_error"]


def test_get_repository_commit_sha(monkeypatch):
    from app.github_client import get_repository_commit_sha

    class FakeCommit:
        sha = "abc123"

    class FakeBranch:
        commit = FakeCommit()

    class FakeRepo:
        def get_branch(self, ref):
            assert ref == "main"
            return FakeBranch()

    class FakeClient:
        def get_repo(self, repository):
            assert repository == "owner/repo"
            return FakeRepo()

        def close(self):
            pass

    monkeypatch.setattr(
        "app.github_client.get_github_client",
        lambda: FakeClient(),
    )

    assert get_repository_commit_sha("owner/repo", "main") == "abc123"

def test_retrieve_context_uses_semantic_search_results():
    state = {
        "repository": "demo/example",
        "branch": "main",
        "issue": "Fix user lookup",
        "use_github": True,
        "supplied_context": "",
    }

    semantic_results = [
        {
            "file_path": "src/UserRepository.java",
            "chunk_index": 0,
            "start_line": 10,
            "end_line": 25,
            "content": "return users.findById(userId);",
            "similarity": 0.91,
        }
    ]

    with (
        patch(
            "app.graph.nodes.get_repository_commit_sha",
            return_value="abc123",
        ),
        patch(
            "app.graph.nodes.index_repository",
            return_value=("abc123", 1),
        ) as mock_index,
        patch(
            "app.graph.nodes.search_similar_chunks",
            return_value=semantic_results,
        ),
        patch(
            "app.graph.nodes.select_relevant_files",
        ) as mock_fallback,
    ):
        result = retrieve_context(state)

    assert "src/UserRepository.java" in result["retrieved_context"]
    assert "Lines: 10-25" in result["retrieved_context"]
    assert "Similarity: 0.910" in result["retrieved_context"]
    assert "return users.findById(userId);" in result["retrieved_context"]

    mock_index.assert_called_once_with(
        repository="demo/example",
        issue="Fix user lookup",
        ref="main",
    )
    mock_fallback.assert_not_called()
