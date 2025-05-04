package org.factelect.dto;

public class CabeceraDTO {
    public String tipoDocumento; // Ej. 01 = Factura, 03 = Boleta, 07 = Nota de Crédito, etc.
    public String serieNumero;
    public String fechaEmision;
    public String horaEmision;
    public String fechaVencimiento;
    public String moneda;
    public String formaPago;
    public String montoEnLetras;
}

/*
"01" → Factura

"03" → Boleta

"07" → Nota de crédito

"08" → Nota de débito

"20" → Retención

"40" → Percepción
*/