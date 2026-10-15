from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_investigation_rejects_empty_issue():
    response = client.post(
        "/investigations",
        json={
            "repository": "demo/example",
            "branch": "main",
            "issue": "",
            "context": "public User getUser(Long id) { return null; }",
        },
    )

    assert response.status_code == 422


def test_investigation_rejects_missing_repository():
    response = client.post(
        "/investigations",
        json={
            "issue": "The user lookup returns the wrong user.",
            "context": "public User getUser(Long id) { return null; }",
        },
    )

    assert response.status_code == 422