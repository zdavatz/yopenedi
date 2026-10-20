package com.ywesee.java.yopenedi.Edifact;

import com.ywesee.java.yopenedi.converter.Converter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.milyn.edi.unedifact.d96a.D96AInterchangeFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

// The additions FEGIME requires for EN 16931 from November 2026, see their
// "Technischer Anhang EDILEKTRO-INVOIC / EN 16931" (October 2026).
public class InvoiceEN16931Test {
    static String edifact;
    static List<String> segments;

    @BeforeClass
    public static void convert() throws Exception {
        InputStream in = InvoiceEN16931Test.class.getResourceAsStream("/opentrans_invoice_en16931.xml");
        com.ywesee.java.yopenedi.OpenTrans.Invoice ot = new com.ywesee.java.yopenedi.OpenTrans.Invoice(in);
        Invoice invoice = new Converter(null).invoiceToEdifact(ot);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        invoice.write(out, StandardCharsets.UTF_8);
        edifact = out.toString("UTF-8");
        segments = Arrays.asList(edifact.split("'"));
    }

    @Test
    public void isValidD96A() throws Exception {
        D96AInterchangeFactory.getInstance().fromUNEdifact(new ByteArrayInputStream(edifact.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void segmentCountMatchesUNT() {
        long counted = segments.stream().filter(s -> !s.startsWith("UNA") && !s.startsWith("UNB") && !s.startsWith("UNZ") && !s.isEmpty()).count();
        assertTrue(segments.contains("UNT+" + counted + "+1"));
    }

    @Test
    public void supplierContact() {
        // 1.3: name, phone and email in one contact; the party's phone fills in
        int cta = segments.indexOf("CTA+AP+:WebShop");
        assertTrue(cta > 0);
        assertEquals("COM+?+49 1234 5678:TE", segments.get(cta + 1));
        assertEquals("COM+verkauf@muster-elektro.de:EM", segments.get(cta + 2));
    }

    @Test
    public void paymentTerms() {
        // 1.4: due date after 60 days, 3% discount within 45 days
        int pat = segments.indexOf("PAT+3");
        assertTrue(pat > 0);
        assertEquals("DTM+13:20261218:102", segments.get(pat + 1));
        assertEquals("DTM+12:20261203:102", segments.get(pat + 2));
        assertTrue(segments.get(pat + 3).matches("PCD\\+12:3(\\.0)?"));
    }

    @Test
    public void bankAccount() {
        // 1.5
        assertTrue(segments.contains("PAI+::42"));
        assertTrue(segments.contains("FII+RB+DE02120300000000202051:Muster Elektro GmbH+BYLADEM1001:25:5+DE"));
    }

    @Test
    public void orderReferenceOnlyOnHeader() {
        // 1.6: a single order goes on the header and not again on the items
        assertTrue(segments.contains("RFF+ON:362946"));
        assertFalse(segments.stream().anyMatch(s -> s.startsWith("RFF+ON:362946:")));
    }

    @Test
    public void itemDeliveryDate() {
        // 1.8
        assertTrue(segments.contains("DTM+35:20260826:102"));
    }

    @Test
    public void taxCategory() {
        // 1.9: 19% is standard rate even though the sender says "exemption"
        assertTrue(segments.contains("TAX+7+VAT+++:::19+S"));
        assertFalse(edifact.contains(":::19.0"));
    }

    @Test
    public void totalsAndTaxPerRate() {
        // 1.10 and 1.11
        int uns = segments.indexOf("UNS+S");
        List<String> summary = segments.subList(uns, segments.size());
        assertTrue(summary.containsAll(Arrays.asList("MOA+86:300.26", "MOA+79:252.32", "MOA+124:47.94", "MOA+125:252.32")));
        int tax = summary.indexOf("TAX+7+VAT+++:::19+S");
        assertTrue(tax > 0);
        assertEquals("MOA+124:47.94", summary.get(tax + 1));
        assertEquals("MOA+125:252.32", summary.get(tax + 2));
    }
}
