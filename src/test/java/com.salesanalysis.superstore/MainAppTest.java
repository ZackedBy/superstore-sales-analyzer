package com.salesanalysis.superstore;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MainAppTest {

    @Test
    public void testMainAppStart() {
        MainApp mainApp = new MainApp();
        assertNotNull(mainApp);
    }
}
