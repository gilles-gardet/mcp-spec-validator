# MCP Spec Validator

Java port of the [OASF SDK validator](https://github.com/agntcy/oasf-sdk/blob/main/pkg/validator/validation.go) for validating MCP server specifications against the [OASF schema server](https://schema.oasf.outshift.com).

## How it works

Records are validated by POSTing JSON to the OASF schema server:

```
POST {schemaBaseUrl}/api/{schemaVersion}/validate/object/record?missing_recommended=true
```

A record is **valid** when the API returns no errors. Warnings are returned but do not affect the validity outcome.

## Record format

An OASF 1.0.0 record for an MCP server embeds the MCP module data inside the `modules` array:

```json
{
  "schema_version": "1.0.0",
  "version": "v0.1.0",
  "name": "org/my-mcp-server",
  "description": "...",
  "authors": ["Author Name"],
  "created_at": "2025-01-01T00:00:00Z",
  "skills": [
    { "name": "natural_language_processing/natural_language_understanding", "id": 101 }
  ],
  "modules": [
    {
      "name": "integration/mcp",
      "data": {
        "name": "my-mcp-server",
        "connections": [
          { "type": "streamable-http", "url": "https://example.com/mcp" }
        ]
      }
    }
  ]
}
```

Connection types: `streamable-http`, `sse`, `stdio`.
See `src/main/resources/samples/` for more examples.

## Configuration

`src/main/resources/application.yaml`:

| Property | Default | Description |
|---|---|---|
| `oasf.validator.schema-base-url` | `https://schema.oasf.outshift.com` | OASF schema server base URL |
| `oasf.validator.default-schema-version` | `1.0.0` | Version used when `schema_version` is absent from the record |
| `oasf.validator.http-timeout-seconds` | `30` | HTTP timeout for calls to the schema server |
| `server.port` | `8060` | Application port |

## Running

```bash
mvn spring-boot:run
```

## REST API

### `POST /validate`

Validates a raw OASF record JSON body.

**Query parameters:**

| Parameter | Required | Description |
|---|---|---|
| `schemaVersion` | No | Overrides the version extracted from the record's `schema_version` field |

**Response codes:**

| Code | Meaning |
|---|---|
| `200 OK` | Record is valid |
| `422 Unprocessable Entity` | Record is invalid — `errors` array is non-empty |

**Response body:**

```json
{
  "valid": true,
  "errors": [],
  "warnings": [
    "Recommended attribute \"modules[0].id\" is missing. Attribute path: modules[0].id."
  ]
}
```

### Examples (HTTPie)

```bash
# Validate a local JSON file
http POST :8060/validate < src/main/resources/samples/valid-mcp-record.json

# With explicit schema version (== is HTTPie's syntax for query parameters)
http POST :8060/validate schemaVersion==1.0.0 < src/main/resources/samples/valid-mcp-record.json
```

### Examples (curl)

```bash
http POST :8060/validate < src/main/resources/samples/valid-mcp-record.json

curl -X POST http://localhost:8060/validate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/samples/valid-mcp-record.json
```

## Sample files

### `samples/valid-mcp-record.json`

```bash
http POST :8060/validate < src/main/resources/samples/valid-mcp-record.json
```

**Expected: `200 OK`, `valid: true`**

A minimal but complete OASF record with all required top-level fields (`schema_version`, `version`, `name`, `description`, `authors`, `created_at`, `skills`) and a valid `streamable-http` connection inside the MCP module.

---

### `samples/valid-mcp-record-stdio.json`

```bash
http POST :8060/validate < src/main/resources/samples/valid-mcp-record-stdio.json
```

**Expected: `200 OK`, `valid: true`**

A valid record using a `stdio` connection type, with `command`, `args`, and `env_vars`. Covers the case of an MCP server launched as a local process (e.g. a Docker-based GitHub MCP server).

---

### `samples/invalid-mcp-record-missing-connections.json`

```bash
http POST :8060/validate < src/main/resources/samples/invalid-mcp-record-missing-connections.json
```

**Expected: `422 Unprocessable Entity`, `valid: false`**

The `connections` array is absent from `modules[0].data`. This field is required by the MCP module schema (`minItems: 1`). The API returns an `attribute_required_missing` error with `attribute_path: modules[0].data.connections`.

---

### `samples/invalid-mcp-record-unknown-field.json`

```bash
http POST :8060/validate < src/main/resources/samples/invalid-mcp-record-unknown-field.json
```

**Expected: `422 Unprocessable Entity`, `valid: false`**

A connection contains an `unknown_field` not defined in the schema. The `mcp_server_connection` object uses `additionalProperties: false`, so any unknown field is rejected. The API returns an `attribute_unknown` error with `attribute_path: modules[0].data.connections[0].unknown_field`.

---

### Schema version override

```bash
http POST :8060/validate schemaVersion==1.0.0 < src/main/resources/samples/valid-mcp-record.json
```

**Expected: `200 OK`, `valid: true`**

Same outcome as the first case, but the schema version is passed explicitly via the `schemaVersion` query parameter instead of being extracted from the record's `schema_version` field. Verifies that the override mechanism works correctly.

## Programmatic usage

```java
@Autowired OasfValidator validator;

// Version extracted from schema_version field in the record
ValidationResult result = validator.validateRecord(recordJson);

// Explicit version override
ValidationResult result = validator.validateRecord(recordJson, "1.0.0");

result.valid();    // true / false
result.errors();   // List<String> — empty when valid
result.warnings(); // List<String> — non-empty even when valid
```
