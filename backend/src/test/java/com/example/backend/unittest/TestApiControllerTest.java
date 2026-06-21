package com.example.backend.unittest;

import com.example.backend.controllers.TestApiController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestApiControllerTest {

    private final TestApiController controller = new TestApiController();

    @Test
    void testAllAccess() {
        assertEquals("Public Content.", controller.allAccess());
    }

    @Test
    void testUserAccess() {
        assertEquals("User Content.", controller.userAccess());
    }

    @Test
    void testAdminAccess() {
        assertEquals("Admin Board.", controller.adminAccess());
    }
}