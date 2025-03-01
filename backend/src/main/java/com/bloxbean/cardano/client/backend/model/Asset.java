package com.bloxbean.cardano.client.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming( PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Asset {

    /**
     * Hex-encoded asset full name
     */
    private String asset;

    /**
     * Policy ID of the asset
     */
    private String policyId;

    /**
     * Hex-encoded asset name of the asset
     */
    private String assetName;

    /**
     * CIP14 based user-facing fingerprint
     */
    private String fingerprint;

    /**
     * Current asset quantity
     */
    private String quantity;

    /**
     * ID of the initial minting transaction
     */
    private String initialMintTxHash;

    /**
     * Count of mint and burn transactions
     */
    private Integer mintOrBurnCount;

    /**
     * On-chain metadata which SHOULD adhere to the valid standards,
     * based on which we perform the look up and display the asset (best effort)
     */
    private JsonNode onchainMetadata;

    /**
     * If on-chain metadata passes validation, we display the standard under which it is valid
     */
    private String onchainMetadataStandard;

    /**
     * Arbitrary plutus data (CIP68).
     */
    private String onchainMetadataExtra;

    /**
     * Asset metadata
     */
    private JsonNode metadata;
}
