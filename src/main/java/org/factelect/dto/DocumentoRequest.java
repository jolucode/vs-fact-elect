package org.factelect.dto;

import java.util.List;

public class DocumentoRequest {
    public CabeceraDTO cabecera;
    public EmisorDTO emisor;
    public ClienteDTO cliente;
    public List<ItemDTO> items;
    public TotalesDTO totales;
    public List<DescuentoGlobalDTO> descuentosGlobales;
    public List<CuotaDTO> cuotas;
    public ReferenciasDTO referencias;
    public FirmaDigitalDTO firmaDigital;
}

