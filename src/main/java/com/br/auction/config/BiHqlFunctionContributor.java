package com.br.auction.config;

import java.math.BigDecimal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registra funcoes HQL auxiliares usadas pelo assistente de B.I. (text-to-HQL), para
 * que a IA nao precise escrever expressoes longas/erradas. Sao apenas funcoes SQL
 * (nao views), portaveis entre Postgres e H2:
 *
 * <ul>
 *   <li>{@code distance_km(lat1, lng1, lat2, lng2)} — distancia em km (haversine). A IA
 *       errava parenteses ao copiar a formula trigonometrica inteira. 0.017453292519943295
 *       = pi/180 (evita radians()/pi()); {@code least(1.0, ...)} protege o acos do dominio.</li>
 *   <li>{@code media_finalizada_modelo(model)} — media HISTORICA (preco real arrematado) do
 *       MESMO modelo: avg do lance de leiloes Finalizados com aquele {@code model} exato.
 *       Evita a IA inflar a media agrupando por ano/marca.</li>
 *   <li>{@code media_finalizada_regiao(model, uf)} — idem, restrito a uma UF ("pela regiao").</li>
 * </ul>
 */
public class BiHqlFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
        BasicType<Double> doubleType = functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE);
        BasicType<BigDecimal> bigDecimalType = functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry().resolve(StandardBasicTypes.BIG_DECIMAL);

        registry.registerPattern(
                "distance_km",
                "(6371 * acos(least(1.0, "
                        + "cos(?1*0.017453292519943295) * cos(?3*0.017453292519943295) "
                        + "* cos((?4-?2)*0.017453292519943295) "
                        + "+ sin(?1*0.017453292519943295) * sin(?3*0.017453292519943295))))",
                doubleType);

        // Media HISTORICA do MESMO modelo (leiloes Finalizados). ?1 = model.
        registry.registerPattern(
                "media_finalizada_modelo",
                "(select round(avg(x.currentbidvalue), 2) from tbauctionitem x "
                        + "join tbauction xa on xa.id = x.fk_id_auction "
                        + "where x.model = ?1 and xa.status = 'Finalizado')",
                bigDecimalType);

        // Idem, restrito a uma UF ("pela regiao"). ?1 = model, ?2 = stateCode.
        registry.registerPattern(
                "media_finalizada_regiao",
                "(select round(avg(x.currentbidvalue), 2) from tbauctionitem x "
                        + "join tbauction xa on xa.id = x.fk_id_auction "
                        + "where x.model = ?1 and xa.status = 'Finalizado' "
                        + "and upper(xa.statecode) = upper(?2))",
                bigDecimalType);
    }
}
