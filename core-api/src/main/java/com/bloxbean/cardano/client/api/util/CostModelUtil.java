package com.bloxbean.cardano.client.api.util;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.plutus.spec.CostMdls;
import com.bloxbean.cardano.client.plutus.spec.CostModel;
import com.bloxbean.cardano.client.plutus.spec.Language;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class CostModelUtil {

    //Conway -- 1st Sept 2024 HF
    private final static long[] plutusV1Costs = new long[]{
            100788,
            420,
            1,
            1,
            1000,
            173,
            0,
            1,
            1000,
            59957,
            4,
            1,
            11183,
            32,
            201305,
            8356,
            4,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            100,
            100,
            16000,
            100,
            94375,
            32,
            132994,
            32,
            61462,
            4,
            72010,
            178,
            0,
            1,
            22151,
            32,
            91189,
            769,
            4,
            2,
            85848,
            228465,
            122,
            0,
            1,
            1,
            1000,
            42921,
            4,
            2,
            24548,
            29498,
            38,
            1,
            898148,
            27279,
            1,
            51775,
            558,
            1,
            39184,
            1000,
            60594,
            1,
            141895,
            32,
            83150,
            32,
            15299,
            32,
            76049,
            1,
            13169,
            4,
            22100,
            10,
            28999,
            74,
            1,
            28999,
            74,
            1,
            43285,
            552,
            1,
            44749,
            541,
            1,
            33852,
            32,
            68246,
            32,
            72362,
            32,
            7243,
            32,
            7391,
            32,
            11546,
            32,
            85848,
            228465,
            122,
            0,
            1,
            1,
            90434,
            519,
            0,
            1,
            74433,
            32,
            85848,
            228465,
            122,
            0,
            1,
            1,
            85848,
            228465,
            122,
            0,
            1,
            1,
            270652,
            22588,
            4,
            1457325,
            64566,
            4,
            20467,
            1,
            4,
            0,
            141992,
            32,
            100788,
            420,
            1,
            1,
            81663,
            32,
            59498,
            32,
            20142,
            32,
            24588,
            32,
            20744,
            32,
            25933,
            32,
            24623,
            32,
            53384111,
            14333,
            10
    };

    private final static long[] plutusV2Costs = new long[]{
            100788,
            420,
            1,
            1,
            1000,
            173,
            0,
            1,
            1000,
            59957,
            4,
            1,
            11183,
            32,
            201305,
            8356,
            4,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            100,
            100,
            16000,
            100,
            94375,
            32,
            132994,
            32,
            61462,
            4,
            72010,
            178,
            0,
            1,
            22151,
            32,
            91189,
            769,
            4,
            2,
            85848,
            228465,
            122,
            0,
            1,
            1,
            1000,
            42921,
            4,
            2,
            24548,
            29498,
            38,
            1,
            898148,
            27279,
            1,
            51775,
            558,
            1,
            39184,
            1000,
            60594,
            1,
            141895,
            32,
            83150,
            32,
            15299,
            32,
            76049,
            1,
            13169,
            4,
            22100,
            10,
            28999,
            74,
            1,
            28999,
            74,
            1,
            43285,
            552,
            1,
            44749,
            541,
            1,
            33852,
            32,
            68246,
            32,
            72362,
            32,
            7243,
            32,
            7391,
            32,
            11546,
            32,
            85848,
            228465,
            122,
            0,
            1,
            1,
            90434,
            519,
            0,
            1,
            74433,
            32,
            85848,
            228465,
            122,
            0,
            1,
            1,
            85848,
            228465,
            122,
            0,
            1,
            1,
            955506,
            213312,
            0,
            2,
            270652,
            22588,
            4,
            1457325,
            64566,
            4,
            20467,
            1,
            4,
            0,
            141992,
            32,
            100788,
            420,
            1,
            1,
            81663,
            32,
            59498,
            32,
            20142,
            32,
            24588,
            32,
            20744,
            32,
            25933,
            32,
            24623,
            32,
            43053543,
            10,
            53384111,
            14333,
            10,
            43574283,
            26308,
            10
    };

    public static final long[] plutusV3Costs = new long[]{
            100788,
            420,
            1,
            1,
            1000,
            173,
            0,
            1,
            1000,
            59957,
            4,
            1,
            11183,
            32,
            201305,
            8356,
            4,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            16000,
            100,
            100,
            100,
            16000,
            100,
            94375,
            32,
            132994,
            32,
            61462,
            4,
            72010,
            178,
            0,
            1,
            22151,
            32,
            91189,
            769,
            4,
            2,
            85848,
            123203,
            7305,
            -900,
            1716,
            549,
            57,
            85848,
            0,
            1,
            1,
            1000,
            42921,
            4,
            2,
            24548,
            29498,
            38,
            1,
            898148,
            27279,
            1,
            51775,
            558,
            1,
            39184,
            1000,
            60594,
            1,
            141895,
            32,
            83150,
            32,
            15299,
            32,
            76049,
            1,
            13169,
            4,
            22100,
            10,
            28999,
            74,
            1,
            28999,
            74,
            1,
            43285,
            552,
            1,
            44749,
            541,
            1,
            33852,
            32,
            68246,
            32,
            72362,
            32,
            7243,
            32,
            7391,
            32,
            11546,
            32,
            85848,
            123203,
            7305,
            -900,
            1716,
            549,
            57,
            85848,
            0,
            1,
            90434,
            519,
            0,
            1,
            74433,
            32,
            85848,
            123203,
            7305,
            -900,
            1716,
            549,
            57,
            85848,
            0,
            1,
            1,
            85848,
            123203,
            7305,
            -900,
            1716,
            549,
            57,
            85848,
            0,
            1,
            955506,
            213312,
            0,
            2,
            270652,
            22588,
            4,
            1457325,
            64566,
            4,
            20467,
            1,
            4,
            0,
            141992,
            32,
            100788,
            420,
            1,
            1,
            81663,
            32,
            59498,
            32,
            20142,
            32,
            24588,
            32,
            20744,
            32,
            25933,
            32,
            24623,
            32,
            43053543,
            10,
            53384111,
            14333,
            10,
            43574283,
            26308,
            10,
            16000,
            100,
            16000,
            100,
            962335,
            18,
            2780678,
            6,
            442008,
            1,
            52538055,
            3756,
            18,
            267929,
            18,
            76433006,
            8868,
            18,
            52948122,
            18,
            1995836,
            36,
            3227919,
            12,
            901022,
            1,
            166917843,
            4307,
            36,
            284546,
            36,
            158221314,
            26549,
            36,
            74698472,
            36,
            333849714,
            1,
            254006273,
            72,
            2174038,
            72,
            2261318,
            64571,
            4,
            207616,
            8310,
            4,
            1293828,
            28716,
            63,
            0,
            1,
            1006041,
            43623,
            251,
            0,
            1
    };

    public final static CostModel PlutusV1CostModel = new CostModel(Language.PLUTUS_V1, plutusV1Costs);
    public final static CostModel PlutusV2CostModel = new CostModel(Language.PLUTUS_V2, plutusV2Costs);
    public final static CostModel PlutusV3CostModel = new CostModel(Language.PLUTUS_V3, plutusV3Costs);

    /**
     * Get language view encoding for costmodels
     * @param costModels
     * @return Language view encoding in bytes
     */
    public static byte[] getLanguageViewsEncoding(CostModel... costModels) {
        CostMdls costMdls = new CostMdls();
        for (CostModel cm : costModels) {
            costMdls.add(cm);
        }

        return costMdls.getLanguageViewEncoding();
    }

    /**
     * Get costmodel for a language from protocol parameters.
     * @param protocolParams
     * @param language
     * @return Optional with costmodel if found, otherwise Optional.empty()
     */
    public static Optional<CostModel> getCostModelFromProtocolParams(ProtocolParams protocolParams, Language language) {
        String languageKey = null;
        if (language == Language.PLUTUS_V1) {
            languageKey = "PlutusV1";
        } else if (language == Language.PLUTUS_V2) {
            languageKey = "PlutusV2";
        } else if (language == Language.PLUTUS_V3) {
            languageKey = "PlutusV3";
        }

        if (protocolParams.getCostModels() == null)
            return Optional.empty();

        LinkedHashMap<String, LinkedHashMap<String, Long>> costModels = protocolParams.getCostModels();
        LinkedHashMap<String, Long> costModelMap = costModels.get(languageKey);
        if (costModelMap == null)
            return Optional.empty();

        boolean sortByAsIntegerKey = costModelMap.containsKey("0") && costModelMap.containsKey("1");
        Stream<Map.Entry<String, Long>> sortedStream;
        if (sortByAsIntegerKey) {
            //Just to make sure the cost model sequence is correct, we sort by key if it is integer
            sortedStream = costModelMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Integer::valueOf)));
        } else {
            //This should be already sorted, so no need to sort
            sortedStream = costModelMap.entrySet().stream();
        }

        long[] costModel = sortedStream
                .map(e -> e.getValue())
                .mapToLong(x -> x)
                .toArray();

        return Optional.of(new CostModel(language, costModel));
    }
}
