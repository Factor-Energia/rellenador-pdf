package com.factorenergia.core.pdfFiller;

import com.factorenergia.core.pdfFiller.dto.HttpItemRequest;
import com.factorenergia.core.pdfFiller.dto.HttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@WebServlet(name = "fill", value = "/pdf/fill")
public class PdfFillerDownloadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            HttpRequest mappedRequest = this.mapJson(request);
            if (this.validate(mappedRequest)) {
                this.processRequest(mappedRequest, response);
            }
        } catch (Exception ex) {
            String exceptionAsString = ex.getMessage();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = String.format("{\"error\": \"Exception has occurred\", \"message\":\"%s\"}", exceptionAsString);

            // Writing the jsonResponse to the Response OutputStream
            try (PrintWriter out = response.getWriter()) {
                out.print(jsonResponse);
            }
        }
    }

    private HttpRequest mapJson(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        String json = sb.toString();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, HttpRequest.class);
    }

    private boolean validate(HttpRequest mappedRequest) {
        return mappedRequest.getTemplate() != null &&
                !mappedRequest.getTemplate().isEmpty() &&
                mappedRequest.getData() != null &&
                !mappedRequest.getData().isEmpty() &&
                mappedRequest.getData().stream().noneMatch(
                        item -> item.getName() == null ||
                                item.getName().isEmpty() ||
                                item.getType() == null ||
                                item.getType().isEmpty() ||
                                item.getValue() == null
                );
    }

    private void attachImage(HttpItemRequest parameterVO, PdfStamper stamper, AcroFields fields) throws IOException, DocumentException {
        Image image;
        if (!parameterVO.getValue().isEmpty()) {
            byte[] imageDataBytes = Base64.getDecoder().decode(parameterVO.getValue());
            image = Image.getInstance(imageDataBytes);

            List<AcroFields.FieldPosition> imageFieldPosition = fields.getFieldPositions(parameterVO.getName());
            Iterator<AcroFields.FieldPosition> itImageFieldPosition = imageFieldPosition.iterator();
            while (itImageFieldPosition.hasNext()) {
                AcroFields.FieldPosition fieldPosition = itImageFieldPosition.next();
                image.setAbsolutePosition(fieldPosition.position.getLeft(), fieldPosition.position.getBottom());
                image.scaleAbsolute(fieldPosition.position.getWidth(), fieldPosition.position.getHeight());
                PdfContentByte content = stamper.getOverContent(fieldPosition.page);
                content.addImage(image);
            }
            fields.removeField(parameterVO.getName());
        }
    }

    private void processRequest(HttpRequest request, HttpServletResponse response) throws ServletException {
        try {
            // Your base64 encoded PDF template
            String base64PdfTemplate = request.getTemplate();

            // decode it
            byte[] pdfBytes = Base64.getDecoder().decode(base64PdfTemplate);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Reads and stamper for existing PDF
            PdfReader reader = new PdfReader(pdfBytes);
            PdfStamper stamper = new PdfStamper(reader, baos);

            // Get the fields from the stamper (write)
            AcroFields form = stamper.getAcroFields();

            // Get All form field names
            Set<String> fldNames = form.getFields().keySet();

            // Loop over each field
            List<HttpItemRequest> dataItems = request.getData();
            for (HttpItemRequest item : dataItems) {
                if (item.getType().equals("string")) {
                    form.setField(item.getName(), item.getValue());
                } else if (item.getType().equals("image")) {
                    attachImage(item, stamper, form);
                }
            }

            // Loop over all fields again to set them to read-only
            for (String fieldName : fldNames) {
                form.setFieldProperty(fieldName, "setfflags", PdfFormField.FF_READ_ONLY, null);
            }

            // close the stamper and reader
            stamper.close();
            reader.close();

            // convert the resulting PDF to Base64
            String base64output = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Return the resulting PDF as a response
            response.setContentType("application/pdf");
            byte[] resultPdf = Base64.getDecoder().decode(base64output);
            response.getOutputStream().write(resultPdf);

        } catch (Exception e) {
            throw new ServletException("Error processing PDF", e);
        }
    }
}
