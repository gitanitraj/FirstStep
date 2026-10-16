package org.firststep.backend.shared.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void shouldWrapDataWhenSuccess() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertTrue(response.success);
        assertEquals("hello", response.data);
        assertNull(response.errorCode);
        assertNull(response.errorMessage);
    }

    @Test
    void shouldSetErrorFieldsWhenError() {
        ApiResponse<String> response = ApiResponse.error("NOT_FOUND", "Resource not found: r1");

        assertFalse(response.success);
        assertNull(response.data);
        assertEquals("NOT_FOUND", response.errorCode);
        assertEquals("Resource not found: r1", response.errorMessage);
    }
}
