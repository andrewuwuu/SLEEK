from pydantic import BaseModel

class HealthData(BaseModel):
    weight: float
    height: float
    age: int
    gender: str