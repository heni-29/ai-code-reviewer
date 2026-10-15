from unittest.mock import patch

from app.graph.retrieval import select_relevant_files


def test_select_relevant_files_prioritizes_matching_filename():
    paths = [
        "src/PaymentService.java",
        "src/UserService.java",
        "src/OrderController.java",
        "README.md",
    ]

    with patch(
        "app.graph.retrieval.get_repository_tree",
        return_value=paths,
    ):
        result = select_relevant_files(
            repository="demo/example",
            issue="Fix the UserService bug",
        )

    assert result[0] == "src/UserService.java"


def test_select_relevant_files_ignores_generated_directories():
    paths = [
        "src/UserService.java",
        "target/UserService.java",
        "node_modules/example.js",
        "build/GeneratedService.java",
    ]

    with patch(
        "app.graph.retrieval.get_repository_tree",
        return_value=paths,
    ):
        result = select_relevant_files(
            repository="demo/example",
            issue="Fix the service bug",
        )

    assert result == ["src/UserService.java"]


def test_select_relevant_files_respects_limit():
    paths = [
        f"src/Service{i}.java"
        for i in range(10)
    ]

    with patch(
        "app.graph.retrieval.get_repository_tree",
        return_value=paths,
    ):
        result = select_relevant_files(
            repository="demo/example",
            issue="Fix the service bug",
            limit=3,
        )

    assert len(result) == 3