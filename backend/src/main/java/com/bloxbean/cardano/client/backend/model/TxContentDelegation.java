package com.bloxbean.cardano.client.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TxContentDelegation {

    /**
     * @deprecated
     * Index of the certificate within the transaction
     */
    private Integer index;

    /**
     * Index of the certificate within the transaction
     */
    private Integer cert_index;

    /**
     * Bech32 delegation stake address
     */
    private String address;

    /**
     * Bech32 ID of delegated stake pool
     */
    private String poolId;

    /**
     * Epoch in which the delegation becomes active
     */
    private Integer activeEpoch;
}
