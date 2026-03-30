# MCP Spec Validator

Validates MCP server specifications against the [OASF MCP module schema](https://schema.oasf.outshift.com/schema/1.0.0/modules/mcp).

## How it works

The input is a single MCP module JSON object.  
It is validated locally against the bundled OASF MCP module schema (JSON Schema Draft-07).

## Module format

```json
{
  "name": "integration/mcp",
  "data": {
    "name": "my-mcp-server",
    "connections": [
      { "type": "streamable-http", "url": "https://example.com/mcp" }
    ]
  }
}
```

Required fields: `name` (must be `"integration/mcp"`), `data.name`, `data.connections` (at least one entry).

Connection types: `streamable-http`, `sse`, `stdio`.

See `src/main/resources/samples/` for more examples.

## Configuration

`src/main/resources/application.yaml`:

| Property | Default | Description |
|---|---|---|
| `oasf.validator.default-schema-version` | `1.0.0` | Schema version used when `schemaVersion` query param is absent |
| `server.port` | `8060` | Application port |

## Running

```bash
mvn spring-boot:run
```

## REST API

### `POST /validate`

Validates a raw MCP module JSON body.

**Query parameters:**

| Parameter | Required | Description |
|---|---|---|
| `schemaVersion` | No | Overrides the default schema version |

**Response codes:**

| Code | Meaning |
|---|---|
| `200 OK` | Module is valid |
| `422 Unprocessable Entity` | Module is invalid — `errors` array is non-empty |

**Response body:**

```json
{
  "valid": false,
  "errors": [
    "$.data.connections: there must be a minimum of 1 items in the array"
  ],
  "warnings": []
}
```

### Examples (HTTPie)

```bash
# Validate a local JSON file
http POST :8060/validate < src/main/resources/samples/valid-mcp-record.json

# With explicit schema version
http POST :8060/validate schemaVersion==1.0.0 < src/main/resources/samples/valid-mcp-record.json
```

### Examples (curl)

```bash
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

A minimal MCP module with a `streamable-http` connection.

---

### `samples/valid-mcp-record-stdio.json`

```bash
http POST :8060/validate < src/main/resources/samples/valid-mcp-record-stdio.json
```

**Expected: `200 OK`, `valid: true`**

A valid module using a `stdio` connection with `command`, `args`, and `env_vars`. Covers the case of an MCP server launched as a local process (e.g. a Docker-based GitHub MCP server).

---

### `samples/invalid-mcp-record-missing-connections.json`

```bash
http POST :8060/validate < src/main/resources/samples/invalid-mcp-record-missing-connections.json
```

**Expected: `422 Unprocessable Entity`, `valid: false`**

The `connections` array is absent from `data`. This field is required by the schema (`minItems: 1`).

---

### `samples/invalid-mcp-record-unknown-field.json`

```bash
http POST :8060/validate < src/main/resources/samples/invalid-mcp-record-unknown-field.json
```

**Expected: `422 Unprocessable Entity`, `valid: false`**

A connection contains an `unknown_field` not defined in the schema. The `mcp_server_connection` object uses `additionalProperties: false`, so any unknown field is rejected.

---

### Schema version override

```bash
http POST :8060/validate schemaVersion==1.0.0 < src/main/resources/samples/valid-mcp-record.json
```

**Expected: `200 OK`, `valid: true`**

The schema version is passed explicitly via the `schemaVersion` query parameter.

## Programmatic usage

```java
@Autowired OasfValidator validator;

// Uses the default schema version from application properties
ValidationResult result = validator.validateModule(moduleJson);

// Explicit version override
ValidationResult result = validator.validateModule(moduleJson, "1.0.0");

result.valid();    // true / false
result.errors();   // List<String> — empty when valid
result.warnings(); // List<String>
```
