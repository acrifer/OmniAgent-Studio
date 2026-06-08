from __future__ import annotations

from typing import Any

import httpx

from app.core.config import settings


class VectorStoreService:
    def __init__(self) -> None:
        self.collection_name = settings.qdrant_collection
        self.base_url = settings.qdrant_url.rstrip("/")
        self.vector_size = 1024

    async def ensure_collection(self, vector_size: int) -> None:
        self.vector_size = vector_size
        payload = {
            "vectors": {
                "size": vector_size,
                "distance": "Cosine",
            }
        }
        async with httpx.AsyncClient(timeout=30) as client:
            exists = await client.get(f"{self.base_url}/collections/{self.collection_name}")
            if exists.status_code == 200:
                return
            response = await client.put(f"{self.base_url}/collections/{self.collection_name}", json=payload)
            response.raise_for_status()

    async def upsert_chunks(self, points: list[dict[str, Any]]) -> None:
        if not points:
            return
        vector_size = len(points[0].get("vector", []))
        if vector_size:
            await self.ensure_collection(vector_size)
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.put(
                f"{self.base_url}/collections/{self.collection_name}/points",
                json={"points": points},
            )
            response.raise_for_status()

    async def delete_document_chunks(self, knowledge_base_id: int, document_id: int) -> None:
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(
                f"{self.base_url}/collections/{self.collection_name}/points/delete",
                json={
                    "filter": {
                        "must": [
                            {"key": "knowledgeBaseId", "match": {"value": knowledge_base_id}},
                            {"key": "documentId", "match": {"value": document_id}},
                        ]
                    }
                },
            )
            response.raise_for_status()

    async def search(
        self,
        knowledge_base_id: int,
        query_vector: list[float],
        top_k: int,
        user_id: int | None = None,
    ) -> list[dict[str, Any]]:
        must_filters: list[dict[str, Any]] = [
            {"key": "knowledgeBaseId", "match": {"value": knowledge_base_id}},
        ]
        if user_id is not None:
            must_filters.append({"key": "userId", "match": {"value": user_id}})
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(
                f"{self.base_url}/collections/{self.collection_name}/points/search",
                json={
                    "vector": query_vector,
                    "limit": top_k,
                    "with_payload": True,
                    "filter": {"must": must_filters},
                },
            )
            response.raise_for_status()
            data = response.json()
        return data.get("result", [])


vector_store = VectorStoreService()
