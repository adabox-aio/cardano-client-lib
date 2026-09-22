package com.bloxbean.cardano.client.backend.nexus;

import adlabs.nexus.client.util.Network;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.TransactionService;
import com.bloxbean.cardano.client.backend.model.TransactionCbor;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.backend.model.TxContentDelegation;
import com.bloxbean.cardano.client.backend.model.TxContentOutputAmount;
import com.bloxbean.cardano.client.backend.model.TxContentRedeemers;
import com.bloxbean.cardano.client.backend.model.TxContentUtxo;
import com.bloxbean.cardano.client.backend.model.TxContentUtxoInputs;
import com.bloxbean.cardano.client.backend.model.TxContentUtxoOutputs;
import com.bloxbean.cardano.client.backend.model.TxContentWithdrawal;
import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.client.util.HexUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Nexus Transaction Service
 */
public class NexusTransactionService implements TransactionService {

    private final adlabs.nexus.client.backend.api.transaction.TransactionService transactionService;
    private final Network network;

    public NexusTransactionService(adlabs.nexus.client.backend.api.transaction.TransactionService transactionService, Network network) {
        this.transactionService = transactionService;
        this.network = network;
    }

    @Override
    public Result<String> submitTransaction(byte[] cborData) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.submitTransaction(network, HexUtil.encodeHexString(cborData)), hash -> hash);
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    @Override
    public Result<TransactionContent> getTransaction(String txnHash) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.getTransaction(network, txnHash), this::toTransactionContent);
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    @Override
    public Result<TransactionCbor> getTransactionCbor(String txnHash) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.getTransactionCbor(network, txnHash), this::toTransactionCbor);
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    @Override
    public Result<List<TransactionCbor>> getTransactionsCbor(List<String> txnHashes) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.getTransactionsCbor(network, txnHashes),
                    cbors -> cbors.stream().map(this::toTransactionCbor).collect(Collectors.toList()));
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    /**
     * Nexus's cbor payload has no {@code utxo} or {@code txSize}, so both stay null.
     */
    private TransactionCbor toTransactionCbor(adlabs.nexus.client.backend.api.transaction.model.TransactionCbor cbor) {
        return TransactionCbor.builder()
                .txHash(cbor.getTxHash())
                .blockHash(cbor.getBlockHash())
                .blockHeight(cbor.getBlockHeight())
                .epochNo(cbor.getEpochNo())
                .absoluteSlot(cbor.getAbsoluteSlot())
                .txTimestamp(toEpochSeconds(cbor.getTxTimestamp()))
                .cbor(cbor.getCbor())
                .build();
    }

    /**
     * Nexus emits txTimestamp as a stringified Unix epoch-seconds value, while the backend model types it
     * as a Long. Parse defensively: anything non-numeric (e.g. an ISO-8601 instant from a future provider)
     * maps to null rather than failing the whole transaction.
     */
    private Long toEpochSeconds(String txTimestamp) {
        if (txTimestamp == null || txTimestamp.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(txTimestamp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Nexus declares a {@code certificates} field on its transaction payload but never populates it
     * (every provider mapper ignores the target), so there is nothing to map. Matches KoiosTransactionService.
     */
    @Override
    public Result<List<TxContentDelegation>> getDelegationCertificates(String txnHash) throws ApiException {
        throw new UnsupportedOperationException("getDelegationCertificates not supported by Nexus");
    }

    /**
     * Withdrawals ride on the transaction payload; Nexus has no dedicated withdrawals endpoint.
     * A transaction with no withdrawals maps to an empty list, not null.
     */
    @Override
    public Result<List<TxContentWithdrawal>> getTransactionWithdrawals(String txnHash) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.getTransaction(network, txnHash), this::toTxContentWithdrawals);
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    private List<TxContentWithdrawal> toTxContentWithdrawals(adlabs.nexus.client.backend.api.transaction.model.Transaction tx) {
        if (tx.getWithdrawals() == null) {
            return new ArrayList<>();
        }
        return tx.getWithdrawals().stream()
                .map(w -> new TxContentWithdrawal(w.getStakeAddr(), w.getAmount()))
                .collect(Collectors.toList());
    }

    // SDK has no batch endpoint; loop per hash and bail on the first failure (mirrors KoiosTransactionService).
    @Override
    public Result<List<TransactionContent>> getTransactions(List<String> txnHashCollection) throws ApiException {
        List<TransactionContent> result = new ArrayList<>();
        for (String txnHash : txnHashCollection) {
            Result<TransactionContent> r = getTransaction(txnHash);
            if (!r.isSuccessful()) {
                // A malformed hash (400) is a caller error and throws, matching Blockfrost;
                // a valid-but-unknown hash (404) surfaces as an unsuccessful Result.
                if (r.code() == 400) {
                    throw new ApiException("Invalid transaction hash in request: " + txnHash);
                }
                return Result.error(r.getResponse()).code(r.code());
            }
            result.add(r.getValue());
        }
        return Result.success("OK").withValue(result).code(200);
    }

    @Override
    public Result<TxContentUtxo> getTransactionUtxos(String txnHash) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.getTransactionUtxos(network, txnHash), this::toTxContentUtxo);
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    @Override
    public Result<List<TxContentRedeemers>> getTransactionRedeemers(String txnHash) throws ApiException {
        try {
            return NexusResultMapper.map(transactionService.getTransaction(network, txnHash),
                    tx -> toRedeemers(tx.getPlutusContracts()));
        } catch (adlabs.nexus.client.backend.api.base.exception.ApiException e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    private List<TxContentRedeemers> toRedeemers(List<adlabs.nexus.client.backend.api.transaction.model.TxPlutusContract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyList();
        }
        return IntStream.range(0, contracts.size())
                .mapToObj(i -> toTxContentRedeemer(i, contracts.get(i)))
                .collect(Collectors.toList());
    }

    private TxContentRedeemers toTxContentRedeemer(int index, adlabs.nexus.client.backend.api.transaction.model.TxPlutusContract c) {
        // No SDK source for redeemerDataHash; leave unmapped.
        adlabs.nexus.client.backend.api.transaction.model.PlutusScriptRedeemer r =
                c.getInput() == null ? null : c.getInput().getRedeemer();
        return TxContentRedeemers.builder()
                .txIndex(index)
                .scriptHash(c.getScriptHash())
                .purpose(r == null || r.getPurpose() == null ? null : RedeemerTag.convert(r.getPurpose().name()))
                .fee(r == null ? null : r.getFee())
                .unitMem(r == null || r.getUnit() == null || r.getUnit().getMem() == null ? null : r.getUnit().getMem().toString())
                .unitSteps(r == null || r.getUnit() == null || r.getUnit().getSteps() == null ? null : r.getUnit().getSteps().toString())
                .datumHash(r == null || r.getDatum() == null ? null : r.getDatum().getHash())
                .redeemerDataHash(null)
                .build();
    }

    private TransactionContent toTransactionContent(adlabs.nexus.client.backend.api.transaction.model.Transaction tx) {
        TransactionContent tc = new TransactionContent();
        tc.setHash(tx.getTxHash());
        tc.setBlock(tx.getBlockHash());
        tc.setBlockHeight(tx.getBlockHeight());
        tc.setBlockTime(tx.getTxTimestamp());
        tc.setSlot(tx.getAbsoluteSlot());
        tc.setFees(tx.getFee());
        tc.setDeposit(tx.getDeposit());
        tc.setSize(tx.getTxSize());
        tc.setInvalidBefore(tx.getInvalidBefore());
        tc.setInvalidHereafter(tx.getInvalidAfter());
        // Nexus has no per-block tx index or Plutus valid-contract flag in this model; leave unmapped.
        tc.setIndex(null);
        tc.setValidContract(null);
        tc.setUtxoCount(utxoCount(tx));
        tc.setWithdrawalCount(tx.getWithdrawals() == null ? null : tx.getWithdrawals().size());
        // Line-item count (number of mint/burn actions, Blockfrost-style) — deliberately not koios's abs-quantity sum.
        tc.setAssetMintOrBurnCount(tx.getAssetsMinted() == null ? null : tx.getAssetsMinted().size());
        return tc;
    }

    // Total UTxO count = inputs + outputs; null only when the model carries neither collection.
    private Integer utxoCount(adlabs.nexus.client.backend.api.transaction.model.Transaction tx) {
        if (tx.getInputs() == null && tx.getOutputs() == null) {
            return null;
        }
        int inputs = tx.getInputs() == null ? 0 : tx.getInputs().size();
        int outputs = tx.getOutputs() == null ? 0 : tx.getOutputs().size();
        return inputs + outputs;
    }

    private TxContentUtxo toTxContentUtxo(adlabs.nexus.client.backend.api.transaction.model.TransactionUtxos txUtxos) {
        TxContentUtxo txContentUtxo = new TxContentUtxo();
        txContentUtxo.setInputs(txUtxos.getInputs() == null ? null :
                txUtxos.getInputs().stream().map(this::toTxContentUtxoInputs).collect(Collectors.toList()));
        txContentUtxo.setOutputs(txUtxos.getOutputs() == null ? null :
                txUtxos.getOutputs().stream().map(this::toTxContentUtxoOutputs).collect(Collectors.toList()));
        return txContentUtxo;
    }

    private TxContentUtxoInputs toTxContentUtxoInputs(adlabs.nexus.client.backend.api.transaction.model.Utxo utxo) {
        return TxContentUtxoInputs.builder()
                .address(utxo.getAddress())
                .amount(toTxContentOutputAmounts(utxo))
                .build();
    }

    private TxContentUtxoOutputs toTxContentUtxoOutputs(adlabs.nexus.client.backend.api.transaction.model.Utxo utxo) {
        return TxContentUtxoOutputs.builder()
                .address(utxo.getAddress())
                .amount(toTxContentOutputAmounts(utxo))
                .outputIndex(utxo.getOutputIndex() == null ? 0 : utxo.getOutputIndex())
                .dataHash(utxo.getDataHash())
                .inlineDatum(utxo.getInlineDatum())
                .referenceScriptHash(utxo.getReferenceScriptHash())
                .build();
    }

    private List<TxContentOutputAmount> toTxContentOutputAmounts(adlabs.nexus.client.backend.api.transaction.model.Utxo utxo) {
        if (utxo.getAmount() == null) {
            return new ArrayList<>();
        }
        return utxo.getAmount().stream()
                .map(a -> new TxContentOutputAmount(a.getUnit(), a.getQuantity()))
                .collect(Collectors.toList());
    }
}
