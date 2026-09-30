from typing import Annotated
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
from fastapi import FastAPI
from pydantic import BaseModel, Field, field_validator
from .demo import recommendations

app = FastAPI(title='TeamForge recommendations', docs_url=None, redoc_url=None)
Text = Annotated[str, Field(min_length=1, max_length=80)]

class Profile(BaseModel):
    displayName: Annotated[str, Field(min_length=1, max_length=60)]
    role: Text
    skills: Annotated[list[Text], Field(min_length=1, max_length=20)]
    interests: Annotated[list[Text], Field(min_length=1, max_length=20)]
    rolesSought: Annotated[list[Text], Field(min_length=1, max_length=10)]
    weeklyHours: Annotated[int, Field(ge=1, le=60)]
    goal: Text
    workingStyle: Text
    timezone: Text
    availability: Annotated[list[Annotated[int, Field(ge=0, le=167)]], Field(min_length=1, max_length=168)]

    @field_validator('timezone')
    @classmethod
    def valid_timezone(cls, value):
        try: ZoneInfo(value)
        except (ZoneInfoNotFoundError, ValueError): raise ValueError('Invalid timezone')
        return value

@app.get('/health')
def health(): return {'status': 'ok', 'modelVersion': 'weighted-reciprocal-v1'}

@app.post('/recommendations/demo')
def demo(profile: Profile):
    return {'accountType': 'DEMO', 'syntheticProfileCount': 264, 'recommendations': recommendations(profile.model_dump())}
