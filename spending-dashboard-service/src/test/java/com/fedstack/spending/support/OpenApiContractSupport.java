package com.fedstack.spending.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates representative HTTP traffic against the checked-in
 * {@code openapi/openapi.yaml} contract.
 *
 * <p>The contract declares {@code openapi: 3.2.0}, a version the available
 * Java OpenAPI-parsing libraries (swagger-parser and its
 * swagger-request-validator consumers) do not yet recognize; they either
 * reject it outright or silently fall back to a legacy Swagger 1.x parser.
 * Rather than downgrade the checked-in contract's declared version to fit
 * older tooling, this loads the contract as plain YAML and validates
 * response bodies against its {@code components.schemas} definitions using
 * a version-agnostic JSON Schema validator. Spec-level 3.2.0 compliance
 * (structure, required attributes, reference resolution) is verified
 * separately by the OpenAPI lint step recorded in evidence.md.
 */
public final class OpenApiContractSupport {
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final JsonNode CONTRACT = readContract();
	private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

	private OpenApiContractSupport() {
	}

	public static JsonNode contract() {
		return CONTRACT;
	}

	/** Confirms the operation and status code are documented at all. */
	public static void assertResponseDocumented(String path, String httpMethod, int status) {
		JsonNode responses = CONTRACT.path("paths").path(path).path(httpMethod.toLowerCase()).path("responses");
		assertThat(responses.has(String.valueOf(status)))
				.describedAs("openapi.yaml must document %s %s -> %d", httpMethod, path, status)
				.isTrue();
	}

	/**
	 * Validates a captured JSON response body against the schema referenced by
	 * {@code paths.<path>.<method>.responses.<status>.content.<mediaType>.schema}
	 * in the checked-in contract.
	 */
	public static void assertBodyMatchesSchema(String path, String httpMethod, int status, String mediaType, String jsonBody) {
		JsonNode responseNode = responseObject(path, httpMethod, status);
		assertThat(responseNode.isMissingNode())
				.describedAs("openapi.yaml must document %s %s -> %d", httpMethod, path, status)
				.isFalse();

		JsonNode schemaNode = responseNode.path("content").path(mediaType).path("schema");
		assertThat(schemaNode.isMissingNode())
				.describedAs("openapi.yaml must document a %s schema for %s %s -> %d", mediaType, httpMethod, path, status)
				.isFalse();

		JsonNode resolvedSchema = resolveRefs(schemaNode.deepCopy());
		((ObjectNode) resolvedSchema).put("$schema", "https://json-schema.org/draft/2020-12/schema");

		SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
		JsonSchema schema = SCHEMA_FACTORY.getSchema(resolvedSchema, config);

		JsonNode instance;
		try {
			instance = JSON.readTree(jsonBody);
		} catch (IOException exception) {
			throw new UncheckedIOException("captured response body is not valid JSON", exception);
		}

		Set<ValidationMessage> errors = schema.validate(instance);
		assertThat(errors)
				.describedAs("response body for %s %s -> %d violates the openapi.yaml %s schema: %s",
						httpMethod, path, status, mediaType, errors)
				.isEmpty();
	}

	/** Confirms the operation documents the given media type for the given status. */
	public static void assertMediaTypeDocumented(String path, String httpMethod, int status, String mediaType) {
		JsonNode content = responseObject(path, httpMethod, status).path("content");
		assertThat(content.has(mediaType))
				.describedAs("openapi.yaml must document %s content for %s %s -> %d", mediaType, httpMethod, path, status)
				.isTrue();
	}

	/** Returns the response object for the given operation and status, resolving a top-level {@code $ref} if present. */
	private static JsonNode responseObject(String path, String httpMethod, int status) {
		JsonNode raw = CONTRACT.path("paths").path(path).path(httpMethod.toLowerCase())
				.path("responses").path(String.valueOf(status));
		if (raw.isMissingNode()) {
			return raw;
		}
		return resolveRefs(raw.deepCopy());
	}

	private static JsonNode resolveRefs(JsonNode node) {
		if (node.isObject()) {
			ObjectNode objectNode = (ObjectNode) node;
			if (objectNode.has("$ref")) {
				String ref = objectNode.get("$ref").asText();
				JsonNode target = resolveJsonPointer(ref);
				return resolveRefs(target.deepCopy());
			}
			objectNode.fields().forEachRemaining(entry -> entry.setValue(resolveRefs(entry.getValue())));
			return objectNode;
		}
		if (node.isArray()) {
			ArrayNode arrayNode = (ArrayNode) node;
			for (int i = 0; i < arrayNode.size(); i++) {
				arrayNode.set(i, resolveRefs(arrayNode.get(i)));
			}
			return arrayNode;
		}
		return node;
	}

	private static JsonNode resolveJsonPointer(String ref) {
		if (!ref.startsWith("#/")) {
			throw new IllegalArgumentException("only local $ref pointers are supported: " + ref);
		}
		String pointer = ref.substring(1);
		JsonNode target = CONTRACT.at(pointer);
		if (target.isMissingNode()) {
			throw new IllegalArgumentException("unresolved $ref in openapi.yaml: " + ref);
		}
		return target;
	}

	private static JsonNode readContract() {
		try {
			String yaml = Files.readString(Path.of("openapi", "openapi.yaml"), StandardCharsets.UTF_8);
			return new ObjectMapper(new YAMLFactory()).readTree(yaml);
		} catch (IOException exception) {
			throw new UncheckedIOException("unable to read openapi/openapi.yaml relative to project directory", exception);
		}
	}
}
