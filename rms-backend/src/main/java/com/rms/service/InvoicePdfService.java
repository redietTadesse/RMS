package com.rms.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.rms.entity.Invoice;
import com.rms.entity.Lease;
import com.rms.entity.Tenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    @Value("${app.base.url:http://localhost:5173}")
    private String baseUrl;

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(79, 70, 229);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb DARK_GRAY = new DeviceRgb(51, 51, 51);

    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdfDoc);

        try {
            Lease lease = invoice.getLease();
            Tenant tenant = lease.getTenant();

            document.add(new Paragraph("INVOICE")
                    .setFontSize(28)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.LEFT));

            document.add(new Paragraph("Rental Management System")
                    .setFontSize(12)
                    .setFontColor(DARK_GRAY)
                    .setMarginBottom(20));

            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));
            detailsTable.setMarginBottom(20);

            detailsTable.addCell(createLabelCell("Invoice Number:"));
            detailsTable.addCell(createValueCell(invoice.getInvoiceNumber()));

            detailsTable.addCell(createLabelCell("Issue Date:"));
            detailsTable.addCell(createValueCell(invoice.getIssueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));

            detailsTable.addCell(createLabelCell("Due Date:"));
            detailsTable.addCell(createValueCell(invoice.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));

            detailsTable.addCell(createLabelCell("Status:"));
            detailsTable.addCell(createValueCell(invoice.getStatus().name()));

            document.add(detailsTable);
            document.add(createTenantSpaceInfo(tenant, lease));
            document.add(createItemsTable(invoice, lease));
            document.add(createTotalTable(invoice));
            addFooter(document);

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private Cell createLabelCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY);
    }

    private Cell createValueCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(10))
                .setBorder(Border.NO_BORDER);
    }

    private Table createTenantSpaceInfo(Tenant tenant, Lease lease) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(20);

        Cell tenantCell = new Cell();
        tenantCell.add(new Paragraph("BILL TO:").setBold().setFontColor(PRIMARY_COLOR).setFontSize(10));
        tenantCell.add(new Paragraph(tenant.getName()).setBold());
        tenantCell.add(new Paragraph("Phone: " + tenant.getPhone()));
        if (tenant.getEmail() != null) {
            tenantCell.add(new Paragraph("Email: " + tenant.getEmail()));
        }
        tenantCell.setBorder(Border.NO_BORDER);
        infoTable.addCell(tenantCell);

        Cell spaceCell = new Cell();
        spaceCell.add(new Paragraph("SPACE DETAILS:").setBold().setFontColor(PRIMARY_COLOR).setFontSize(10));
        spaceCell.add(new Paragraph("Unit: " + lease.getSpace().getUnitNumber()).setBold());
        spaceCell.add(new Paragraph("Category: " + lease.getSpace().getCategory()));
        spaceCell.add(new Paragraph("Floor: " + lease.getSpace().getFloor()));
        spaceCell.add(new Paragraph("Size: " + lease.getSpace().getSize() + " sqm"));
        spaceCell.setBorder(Border.NO_BORDER);
        infoTable.addCell(spaceCell);

        return infoTable;
    }

    private Table createItemsTable(Invoice invoice, Lease lease) {
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));
        itemsTable.setMarginBottom(10);

        itemsTable.addHeaderCell(createHeaderCell("Description"));
        itemsTable.addHeaderCell(createHeaderCell("Amount (ETB)"));

        itemsTable.addCell(createItemCell("Monthly Rent - Unit " + lease.getSpace().getUnitNumber()));
        itemsTable.addCell(createAmountCell(invoice.getRentAmount()));

        if (invoice.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
            itemsTable.addCell(createItemCell("Late Payment Penalty"));
            itemsTable.addCell(createAmountCell(invoice.getPenaltyAmount()));
        }

        return itemsTable;
    }

    private Table createTotalTable(Invoice invoice) {
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
        totalTable.setWidth(UnitValue.createPercentValue(100));
        totalTable.setMarginBottom(20);

        totalTable.addCell(new Cell()
                .add(new Paragraph("TOTAL:").setBold().setFontSize(14).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY));
        totalTable.addCell(new Cell()
                .add(new Paragraph("ETB " + formatAmount(invoice.getTotalAmount())).setBold().setFontSize(14))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY)
                .setFontColor(PRIMARY_COLOR));

        return totalTable;
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("PAYMENT INFORMATION:")
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setFontSize(10)
                .setMarginTop(10));

        document.add(new Paragraph("Pay online: " + baseUrl + "/payments")
                .setFontSize(10));

        document.add(new Paragraph("Or upload your receipt through the tenant portal.")
                .setFontSize(9)
                .setFontColor(DARK_GRAY)
                .setMarginBottom(20));

        document.add(new Paragraph("This is a computer-generated invoice.")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));

        document.add(new Paragraph("RMS - Rental Management System")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private Cell createItemCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(10))
                .setPadding(8);
    }

    private Cell createAmountCell(BigDecimal amount) {
        return new Cell()
                .add(new Paragraph(formatAmount(amount)).setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(8);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}