package org.factelect.builder;

// ==== 4. XML Builder ====
// src/main/java/com/empresa/service/XmlBuilder.java

import jakarta.enterprise.context.ApplicationScoped;
import org.factelect.dto.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class XmlBuilder {

    public String buildInvoiceXml(DocumentoRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\" ")
                .append("xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\" ")
                .append("xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\" ")
                .append("xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\" ")
                .append("xmlns:ns6=\"urn:oasis:names:specification:ubl:schema:xsd:SignatureBasicComponents-2\" ")
                .append("xmlns:ns7=\"http://www.w3.org/2000/09/xmldsig#\" ")
                .append("xmlns:ns8=\"urn:oasis:names:specification:ubl:schema:xsd:SignatureAggregateComponents-2\" ")
                .append("xmlns:ns9=\"urn:oasis:names:specification:ubl:schema:xsd:CommonSignatureComponents-2\" ")
                .append("xmlns:sac=\"urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1\">");

        // Firma digital con <ext:UBLExtensions>
        sb.append(buildFirmaDigital(request.firmaDigital));

        sb.append("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>");
        sb.append("<cbc:CustomizationID>2.0</cbc:CustomizationID>");
        sb.append("""
                <cbc:ProfileID schemeAgencyName="PE:SUNAT"
                               schemeName="Tipo de Operacion"
                               schemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo51">0101</cbc:ProfileID>
                """);
        sb.append(String.format("<cbc:ID>%s</cbc:ID>", request.cabecera.serieNumero));
        sb.append(String.format("<cbc:IssueDate>%s</cbc:IssueDate>", request.cabecera.fechaEmision));
        sb.append(String.format("<cbc:IssueTime>%s</cbc:IssueTime>", request.cabecera.horaEmision));
        sb.append(String.format("<cbc:DueDate>%s</cbc:DueDate>", request.cabecera.fechaVencimiento)); // si aplica
        sb.append(String.format("""
                        <cbc:InvoiceTypeCode listAgencyName="PE:SUNAT"
                                             listID="0101"
                                             listName="Tipo de Documento"
                                             listSchemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo51"
                                             listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01"
                                             name="Tipo de Operacion">%s</cbc:InvoiceTypeCode>
                        """,
                request.cabecera.tipoDocumento));
        sb.append(String.format("<cbc:Note languageLocaleID=\"1000\">%s</cbc:Note>",
                request.cabecera.montoEnLetras));
        sb.append(String.format("""
                        <cbc:DocumentCurrencyCode listAgencyName="United Nations Economic Commission for Europe"
                                                  listID="ISO 4217 Alpha"
                                                  listName="Currency">%s</cbc:DocumentCurrencyCode>
                        """,
                request.cabecera.moneda));
        sb.append(String.format("<cbc:LineCountNumeric>%d</cbc:LineCountNumeric>",
                request.items.size()));

        sb.append(buildSignature(request.emisor));
        sb.append(buildEmisor(request.emisor));
        sb.append(buildCliente(request.cliente));
        sb.append(buildFormaPago(request.cabecera, request.cuotas));
        sb.append(buildTaxTotal(BigDecimal.valueOf(request.totales.subtotal), BigDecimal.valueOf(request.totales.igv)));
        sb.append(buildTotales(request.totales));

        int index = 1;
        for (ItemDTO item : request.items) {
            sb.append(buildItem(item, index));
            index++;
        }

        sb.append("</Invoice>");
        Files.writeString(Path.of("C:\\Users\\DESARROLLO_SDK\\Documents\\GitHub\\xml-factura.xml"), sb.toString(), StandardCharsets.UTF_8);
        return sb.toString();
    }

    private String buildFormaPago(CabeceraDTO cabecera, List<CuotaDTO> cuotas) {
        StringBuilder sb = new StringBuilder();

        if ("Credito".equalsIgnoreCase(cabecera.formaPago) && cuotas != null && !cuotas.isEmpty()) {
            // Total de cuotas: se requiere para <cbc:Amount> en la forma de pago general
            double totalCuotas = cuotas.stream().mapToDouble(c -> c.monto).sum();

            // Forma de pago principal (obligatoriamente con <cbc:Amount>)
            sb.append(String.format("""
                    <cac:PaymentTerms>
                        <cbc:ID>FormaPago</cbc:ID>
                        <cbc:PaymentMeansID>Credito</cbc:PaymentMeansID>
                        <cbc:Amount currencyID="PEN">%.2f</cbc:Amount>
                    </cac:PaymentTerms>
                    """, totalCuotas));

            // Cuotas individuales con ID y fecha
            for (CuotaDTO cuota : cuotas) {
                sb.append(String.format("""
                                <cac:PaymentTerms>
                                    <cbc:ID>FormaPago</cbc:ID>
                                    <cbc:PaymentMeansID>%s</cbc:PaymentMeansID>
                                    <cbc:Amount currencyID="PEN">%.2f</cbc:Amount>
                                    <cbc:PaymentDueDate>%s</cbc:PaymentDueDate>
                                </cac:PaymentTerms>
                                """,
                        cuota.id,
                        cuota.monto,
                        cuota.fechaVencimiento));
            }

        } else {
            // Contado
            sb.append("""
                    <cac:PaymentTerms>
                        <cbc:ID>FormaPago</cbc:ID>
                        <cbc:PaymentMeansID>Contado</cbc:PaymentMeansID>
                    </cac:PaymentTerms>
                    """);
        }
        return sb.toString();
    }

    private String buildTaxTotal(BigDecimal baseImponible, BigDecimal igv) {
        return String.format("""
                        <cac:TaxTotal>
                            <cbc:TaxAmount currencyID="PEN">%.2f</cbc:TaxAmount>
                            <cac:TaxSubtotal>
                                <cbc:TaxableAmount currencyID="PEN">%.2f</cbc:TaxableAmount>
                                <cbc:TaxAmount currencyID="PEN">%.2f</cbc:TaxAmount>
                                <cac:TaxCategory>
                                    <cac:TaxScheme>
                                        <cbc:ID schemeID="UN/ECE 5153" schemeName="Codigo de tributos">1000</cbc:ID>
                                        <cbc:Name>IGV</cbc:Name>
                                        <cbc:TaxTypeCode>VAT</cbc:TaxTypeCode>
                                    </cac:TaxScheme>
                                </cac:TaxCategory>
                            </cac:TaxSubtotal>
                        </cac:TaxTotal>
                        """,
                igv,
                baseImponible,
                igv);
    }


    private String buildSignature(EmisorDTO emisor) {
        return String.format("""
                        <cac:Signature>
                            <cbc:ID>signer%s</cbc:ID>
                            <cac:SignatoryParty>
                                <cac:PartyIdentification>
                                    <cbc:ID>%s</cbc:ID>
                                </cac:PartyIdentification>
                                <cac:PartyName>
                                    <cbc:Name>%s</cbc:Name>
                                </cac:PartyName>
                            </cac:SignatoryParty>
                            <cac:DigitalSignatureAttachment>
                                <cac:ExternalReference>
                                    <cbc:URI>#signer%s</cbc:URI>
                                </cac:ExternalReference>
                            </cac:DigitalSignatureAttachment>
                        </cac:Signature>
                        """,
                emisor.ruc,
                emisor.ruc,
                emisor.razonSocial,
                emisor.ruc
        );
    }


    private String buildEmisor(EmisorDTO emisor) {
        return String.format("""
                        <cac:AccountingSupplierParty>
                            <cac:Party>
                                <cac:PartyIdentification>
                                    <cbc:ID schemeID="%s"
                                            schemeName="Documento de Identidad"
                                            schemeAgencyName="PE:SUNAT"
                                            schemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06">%s</cbc:ID>
                                </cac:PartyIdentification>
                                <cac:PartyName>
                                    <cbc:Name>%s</cbc:Name>
                                </cac:PartyName>
                                <cac:PartyLegalEntity>
                                    <cbc:RegistrationName>%s</cbc:RegistrationName>
                                    <cac:RegistrationAddress>
                                        <cbc:ID schemeAgencyName="PE:INEI" schemeName="Ubigeos">%s</cbc:ID>
                                        <cbc:AddressTypeCode listAgencyName="PE:SUNAT" listName="Establecimientos anexos">%s</cbc:AddressTypeCode>
                                        <cbc:CityName>%s</cbc:CityName>
                                        <cbc:CountrySubentity>%s</cbc:CountrySubentity>
                                        <cbc:District>%s</cbc:District>
                                        <cac:AddressLine>
                                            <cbc:Line>%s</cbc:Line>
                                        </cac:AddressLine>
                                        <cac:Country>
                                            <cbc:IdentificationCode listAgencyName="United Nations Economic Commission for Europe" listID="ISO 3166-1" listName="Country">PE</cbc:IdentificationCode>
                                        </cac:Country>
                                    </cac:RegistrationAddress>
                                </cac:PartyLegalEntity>
                            </cac:Party>
                        </cac:AccountingSupplierParty>
                        """,
                emisor.tipoDocumento,
                emisor.ruc,
                emisor.razonSocial,
                emisor.razonSocial,
                emisor.ubigeo,
                emisor.codigoEstablecimiento,
                emisor.ciudad,
                emisor.departamento,
                emisor.distrito,
                emisor.direccionCompleta
        );
    }

    private String buildCliente(ClienteDTO dto) {
        return String.format("""
                        <cac:AccountingCustomerParty>
                            <cac:Party>
                                <cac:PartyIdentification>
                                    <cbc:ID schemeID="%s" schemeName="Documento de Identidad" schemeAgencyName="PE:SUNAT" schemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06">%s</cbc:ID>
                                </cac:PartyIdentification>
                                <cac:PartyName>
                                    <cbc:Name>%s</cbc:Name>
                                </cac:PartyName>
                                <cac:PartyLegalEntity>
                                    <cbc:RegistrationName>%s</cbc:RegistrationName>
                                </cac:PartyLegalEntity>
                            </cac:Party>
                        </cac:AccountingCustomerParty>
                        """,
                dto.tipoDocumento,
                dto.ruc.trim(),
                dto.razonSocial,
                dto.razonSocial
        );
    }

    private String buildItem(ItemDTO item, int index) {
        double baseImponible = item.cantidad * item.precioUnitario;
        double igv = baseImponible * 0.18;
        double precioConIgv = item.precioUnitario * 1.18;

        return String.format("""
                        <cac:InvoiceLine>
                            <cbc:ID>%d</cbc:ID>
                            <cbc:InvoicedQuantity unitCode="%s">%.2f</cbc:InvoicedQuantity>
                            <cbc:LineExtensionAmount currencyID="PEN">%.2f</cbc:LineExtensionAmount>
                        
                            <cac:PricingReference>
                                <cac:AlternativeConditionPrice>
                                    <cbc:PriceAmount currencyID="PEN">%.2f</cbc:PriceAmount>
                                    <cbc:PriceTypeCode listName="Tipo de Precio" listAgencyName="PE:SUNAT"
                                        listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo16">01</cbc:PriceTypeCode>
                                </cac:AlternativeConditionPrice>
                            </cac:PricingReference>
                        
                            <cac:TaxTotal>
                                <cbc:TaxAmount currencyID="PEN">%.2f</cbc:TaxAmount>
                                <cac:TaxSubtotal>
                                    <cbc:TaxableAmount currencyID="PEN">%.2f</cbc:TaxableAmount>
                                    <cbc:TaxAmount currencyID="PEN">%.2f</cbc:TaxAmount>
                                    <cac:TaxCategory>
                                    <cbc:Percent>18.00</cbc:Percent>
                                        <cbc:TaxExemptionReasonCode>10</cbc:TaxExemptionReasonCode>
                                        <cac:TaxScheme>
                                            <cbc:ID>1000</cbc:ID>
                                            <cbc:Name>IGV</cbc:Name>
                                            <cbc:TaxTypeCode>VAT</cbc:TaxTypeCode>
                                        </cac:TaxScheme>
                                    </cac:TaxCategory>
                                </cac:TaxSubtotal>
                            </cac:TaxTotal>
                        
                            <cac:Item>
                                <cbc:Description>%s</cbc:Description>
                            </cac:Item>
                        
                            <cac:Price>
                                <cbc:PriceAmount currencyID="PEN">%.2f</cbc:PriceAmount>
                            </cac:Price>
                        </cac:InvoiceLine>
                        """,
                index,
                item.unidadMedida,
                item.cantidad,
                baseImponible,
                precioConIgv,
                igv,
                baseImponible,
                igv,
                item.descripcion,
                item.precioUnitario
        );
    }

    private String buildTotales(TotalesDTO dto) {
        return String.format("""
                        <cac:LegalMonetaryTotal>
                            <cbc:LineExtensionAmount currencyID="PEN">%.2f</cbc:LineExtensionAmount>
                            <cbc:TaxInclusiveAmount currencyID="PEN">%.2f</cbc:TaxInclusiveAmount>
                            <cbc:PayableAmount currencyID="PEN">%.2f</cbc:PayableAmount>
                        </cac:LegalMonetaryTotal>
                        """,
                dto.subtotal,
                dto.subtotal + dto.igv,
                dto.total
        );
    }

    private String buildFirmaDigital(FirmaDigitalDTO firma) {
        return String.format("""
                        <ext:UBLExtensions>
                            <ext:UBLExtension>
                                <ext:ExtensionContent>
                                    <sac:AdditionalInformation/>
                                </ext:ExtensionContent>
                            </ext:UBLExtension>
                            <ext:UBLExtension>
                                <ext:ExtensionContent>
                                    <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#" Id="signer%s">
                                        <ds:SignedInfo>
                                            <ds:CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
                                            <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
                                            <ds:Reference URI="">
                                                <ds:Transforms>
                                                    <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                                                </ds:Transforms>
                                                <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
                                                <ds:DigestValue>%s</ds:DigestValue>
                                            </ds:Reference>
                                        </ds:SignedInfo>
                                        <ds:SignatureValue>%s</ds:SignatureValue>
                                        <ds:KeyInfo>
                                            <ds:X509Data>
                                                <ds:X509Certificate>%s</ds:X509Certificate>
                                            </ds:X509Data>
                                        </ds:KeyInfo>
                                    </ds:Signature>
                                </ext:ExtensionContent>
                            </ext:UBLExtension>
                        </ext:UBLExtensions>
                        """,
                firma != null ? firma.x509Certificate.substring(0, 11) : "x509CertificatePrueba",
                firma != null ? firma.digestValue : "digestValuePrueba",
                firma != null ? firma.signatureValue : "signatureValuePrueba",
                firma != null ? firma.x509Certificate : "x509CertificatePrueba"
        );
    }
}