from pathlib import PurePosixPath

from app.github_client import get_repository_tree


SOURCE_EXTENSIONS = {
    ".java", ".py", ".js", ".jsx", ".ts", ".tsx",
    ".go", ".rs", ".cpp", ".c", ".h", ".cs",
    ".kt", ".swift", ".rb", ".php",
}

TEST_KEYWORDS = ("test", "tests", "spec", "specs")

IGNORED_DIRECTORIES = {
    ".git",
    "node_modules",
    "vendor",
    "build",
    "dist",
    "target",
    "__pycache__",
    ".venv",
}


def select_relevant_files(
    repository: str,
    issue: str,
    ref: str = "main",
    limit: int = 5,
) -> list[str]:
    if limit <= 0:
        return []

    paths = get_repository_tree(repository, ref)

    issue_words = {
        word.lower()
        for word in issue.replace("_", " ").replace("-", " ").split()
        if len(word) > 2
    }

    production_candidates = []
    test_candidates = []

    for path in paths:
        file_path = PurePosixPath(path)
        parts = [part.lower() for part in file_path.parts]
        filename = file_path.name.lower()

        if any(
            part in IGNORED_DIRECTORIES
            for part in parts[:-1]
        ):
            continue

        if file_path.suffix.lower() not in SOURCE_EXTENSIONS:
            continue

        is_test = (
            any(keyword in filename for keyword in TEST_KEYWORDS)
            or any(
                part in {"test", "tests", "spec", "specs"}
                for part in parts[:-1]
            )
        )

        matching_filename_words = sum(
            1 for word in issue_words if word in filename
        )

        matching_path_words = sum(
            1
            for word in issue_words
            if any(word in part for part in parts[:-1])
        )

        score = (
            matching_filename_words * 4
            + matching_path_words * 2
        )

        candidate = (score, path)

        if is_test:
            test_candidates.append(candidate)
        else:
            production_candidates.append(candidate)

    production_candidates.sort(
        key=lambda item: (-item[0], item[1])
    )
    test_candidates.sort(
        key=lambda item: (-item[0], item[1])
    )

    # Prefer issue-relevant production code first.
    selected = [
        path
        for score, path in production_candidates
        if score > 0
    ][:limit]

    # Add tests only when they match the issue and space remains.
    if len(selected) < limit:
        selected.extend(
            path
            for score, path in test_candidates
            if score > 0 and path not in selected
        )

    # Fall back to production files if no filename matches.
    if not selected:
        selected = [
            path
            for _, path in production_candidates[:limit]
        ]

    return selected[:limit]