package com.bloxbean.cardano.client.backend.blockfrost.service;

import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.TransactionService;
import com.bloxbean.cardano.client.backend.api.UtxoService;
import com.bloxbean.cardano.client.backend.blockfrost.service.http.AddressesApi;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BFUtxoService extends BFBaseService implements UtxoService {

    private final AddressesApi addressApi;
    private final TransactionService transactionService;

    public BFUtxoService(String baseUrl, String projectId) {
        super(baseUrl, projectId);
        this.addressApi = getRetrofit().create(AddressesApi.class);
        this.transactionService = new BFTransactionService(baseUrl, projectId);
    }

    @Override
    public Result<List<Utxo>> getUtxos(String address, int count, int page) throws ApiException {
        return getUtxos(address, count, page, OrderEnum.asc);
    }

    public Result<List<Utxo>> getUtxos(String address, int count, int page, OrderEnum order) throws ApiException {
        Call<List<Utxo>> utxosCall = addressApi.getUtxos(getProjectId(), address, count, page, order.toString());
        try {
            Response<List<Utxo>> response = utxosCall.execute();
            return processResponse(response);

        } catch (IOException e) {
            throw new ApiException("Error getting utxos", e);
        }
    }

    @Override
    public Result<List<Utxo>> getAllUtxos(String address) throws ApiException {
        int count = 100; // Maximum number of items per page (Blockfrost allows up to 100)
        int page = 1;
        List<Utxo> allUtxos = new ArrayList<>();
        while (true) {
            Result<List<Utxo>> result = getUtxos(address, count, page, OrderEnum.asc);
            if (!result.isSuccessful()) {
                return result;
            }
            List<Utxo> utxos = result.getValue();
            if (utxos == null || utxos.isEmpty()) {
                break;
            }
            allUtxos.addAll(utxos);
            if (utxos.size() < count) {
                break;
            }
            page++;
        }
        return Result.success("OK").withValue(allUtxos).code(200);
    }

    @Override
    public Result<List<Utxo>> getUtxos(String address, String unit, int count, int page) throws ApiException {
        return getUtxos(address, unit, count, page, OrderEnum.asc);
    }

    @Override
    public Result<List<Utxo>> getUtxos(String address, String unit, int count, int page, OrderEnum order) throws ApiException {
        Call<List<Utxo>> utxosCall = addressApi.getUtxosByAsset(getProjectId(), address, unit, count, page, order.toString());

        try {
            Response<List<Utxo>> response = utxosCall.execute();
            return processResponse(response);
        } catch (IOException e) {
            throw new ApiException("Error getting utxos for address : " + address + ", asset: " + unit, e);
        }
    }

    @Override
    public Result<Utxo> getTxOutput(String txHash, int outputIndex) throws ApiException {
        return transactionService.getTransactionOutput(txHash, outputIndex);
    }
}
