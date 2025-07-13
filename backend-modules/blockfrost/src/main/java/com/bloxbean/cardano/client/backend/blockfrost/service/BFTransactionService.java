package com.bloxbean.cardano.client.backend.blockfrost.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.TransactionService;
import com.bloxbean.cardano.client.backend.blockfrost.service.http.TransactionApi;
import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.backend.model.*;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.util.HexUtil;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class BFTransactionService extends BFBaseService implements TransactionService {

    private final TransactionApi transactionApi;

    public BFTransactionService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.transactionApi = getRetrofit().create(TransactionApi.class);
    }

    @Override
    public Result<String> submitTransaction(byte[] cborData) throws ApiException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/cbor"), cborData);
        Call<String> txnCall = transactionApi.submit(getProjectId(), requestBody);
        try {
            Response<String> response = txnCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error submit transaction", e);
        }
    }

    @Override
    public Result<TransactionContent> getTransaction(String txnHash) throws ApiException {
        Call<TransactionContent> txnCall = transactionApi.getTransaction(getProjectId(), txnHash);
        try {
            Response<TransactionContent> response = txnCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error getting transaction for id : " + txnHash, e);
        }
    }

    @Override
    public Result<List<TransactionContent>> getTransactions(List<String> txnHashCollection) throws ApiException {
        List<TransactionContent> transactionContentList = new ArrayList<>();
        for (String txnHash : txnHashCollection) {
            if (!txnHash.isEmpty() && !txnHash.matches("^[\\da-fA-F]+$")) {
                throw new ApiException("Invalid Transaction Hash Format");
            }
            Result<TransactionContent> result = getTransaction(txnHash);
            if (result.isSuccessful()) {
                transactionContentList.add(result.getValue());
            } else {
                return Result.error(result.getResponse()).code(result.code());
            }
        }
        return Result.success("OK").withValue(transactionContentList).code(200);
    }

    @Override
    public Result<TransactionCbor> getTransactionCbor(String txnHash) throws ApiException {
        Call<TransactionCbor> txnCall = transactionApi.getTransactionCbor(getProjectId(), txnHash);
        try {
            Response<TransactionCbor> response = txnCall.execute();
            if (response.isSuccessful()) {
                TransactionCbor transactionCbor = response.body();
                Objects.requireNonNull(transactionCbor).setTxHash(txnHash);
                return Result.success(response.toString()).withValue(transactionCbor).code(response.code());
            } else {
                return Result.error(response.errorBody().string()).code(response.code());
            }
        } catch (IOException e) {
            throw new ApiException("Error getting transaction cbor for id : " + txnHash, e);
        }
    }

    @Override
    public Result<List<TransactionCbor>> getTransactionsCbor(List<String> txnHashes) throws ApiException {
        List<CompletableFuture<TransactionCbor>> futures = txnHashes.stream()
                .map(txnHash -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Result<TransactionCbor> cborRes = getTransactionCbor(txnHash);
                        Result<TransactionContent> contentRes = getTransaction(txnHash);
                        Result<TxContentUtxo> utxoRes = getTransactionUtxos(txnHash);

                        if (!cborRes.isSuccessful()) throw new ApiException(cborRes.getResponse());
                        if (!contentRes.isSuccessful()) throw new ApiException(contentRes.getResponse());
                        if (!utxoRes.isSuccessful()) throw new ApiException(utxoRes.getResponse());

                        return buildTransactionCbor(cborRes, contentRes, utxoRes);
                    } catch (ApiException e) {
                        throw new CompletionException(e);
                    }
                })).collect(Collectors.toList());
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException ce) {
            if (ce.getCause() instanceof ApiException) {
                throw (ApiException) ce.getCause();
            }
            throw ce;
        }

        List<TransactionCbor> resultList = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return Result.success("OK").withValue(resultList).code(200);
    }

    @NotNull
    private static TransactionCbor buildTransactionCbor(Result<TransactionCbor> cborRes, Result<TransactionContent> contentRes, Result<TxContentUtxo> utxoRes) {
        TransactionCbor tc = new TransactionCbor();
        tc.setCbor(cborRes.getValue().getCbor());
        tc.setBlockHash(contentRes.getValue().getBlock());
        tc.setBlockHeight(contentRes.getValue().getBlockHeight());
        tc.setAbsoluteSlot(cborRes.getValue().getAbsoluteSlot());
        tc.setTxTimestamp(contentRes.getValue().getBlockTime());
        tc.setTxSize(contentRes.getValue().getSize());
        tc.setTxHash(cborRes.getValue().getTxHash());
        tc.setUtxo(utxoRes.getValue());
        return tc;
    }

    @Override
    public Result<TxContentUtxo> getTransactionUtxos(String txnHash) throws ApiException {
        Call<TxContentUtxo> txnCall = transactionApi.getTransactionUtxos(getProjectId(), txnHash);
        try {
            Response<TxContentUtxo> response = txnCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error getting transaction utxos for id : " + txnHash, e);
        }
    }

    @Override
    public Result<List<TxContentDelegation>> getDelegationCertificates(String txnHash) throws ApiException {
        Call<List<TxContentDelegation>> txnCall = transactionApi.getDelegationCertificates(getProjectId(), txnHash);
        try {
            Response<List<TxContentDelegation>> response = txnCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error getting transaction delegation certificates for hash: " + txnHash, e);
        }
    }

    @Override
    public Result<List<TxContentWithdrawal>> getTransactionWithdrawals(String txnHash) throws ApiException {
        Call<List<TxContentWithdrawal>> txnCall = transactionApi.getTransactionWithdrawals(getProjectId(), txnHash);
        try {
            Response<List<TxContentWithdrawal>> response = txnCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error getting transaction withdrawals for hash: " + txnHash, e);
        }
    }

    @Override
    public Result<List<TxContentRedeemers>> getTransactionRedeemers(String txnHash) throws ApiException {
        Call<List<TxContentRedeemers>> txnCall = transactionApi.getTransactionRedeemers(getProjectId(), txnHash);
        try {
            Response<List<TxContentRedeemers>> response = txnCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error getting transaction redeemers for id : " + txnHash, e);
        }
    }

    @Override
    public Result<List<EvaluationResult>> evaluateTx(byte[] cborData) throws ApiException {

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/cbor"), HexUtil.encodeHexString(cborData));

        Call<Object> evalCall = transactionApi.evaluateTx(getProjectId(), requestBody);
        try {
            Response<Object> response = evalCall.execute();
            return OgmiosTxResponseParser.processEvaluateResponse(response);

        } catch (IOException e) {
            throw new ApiException("Error evaluating script cost for transaction", e);
        }
    }

}
