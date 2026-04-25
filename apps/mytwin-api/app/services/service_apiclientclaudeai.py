import os
from anthropic import Anthropic

client = Anthropic(api_key=os.getenv("CLAUDEAI_KEY"))

async def call_claude(prompt):
    response = client.messages.create(
        model="claude-3-sonnet",
        max_tokens=500,
        system=prompt["system"],
        messages=[
            {
                "role": "user",
                "content": prompt["user"]
            }
        ]
    )

    return response.content[0].text