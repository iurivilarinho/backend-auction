package com.br.auction.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registra a funcao HQL {@code distance_km(lat1, lng1, lat2, lng2)} que devolve a
 * distancia em quilometros entre dois pontos (formula de haversine), para que a IA
 * do B.I. possa filtrar/ordenar por distancia sem precisar escrever a expressao
 * trigonometrica inteira (que e longa e o modelo erra parenteses). Usa apenas
 * funcoes matematicas padrao (cos/sin/acos/least), portavel entre Postgres e H2.
 *
 * <p>Args (graus decimais): ?1 = latitude origem, ?2 = longitude origem,
 * ?3 = latitude destino, ?4 = longitude destino. 0.017453292519943295 = pi/180
 * (evita depender de radians()/pi()). O {@code least(1.0, ...)} protege o acos de
 * estourar o dominio por erro de ponto flutuante quando os pontos coincidem.</p>
 */
public class DistanceFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "distance_km",
                "(6371 * acos(least(1.0, "
                        + "cos(?1*0.017453292519943295) * cos(?3*0.017453292519943295) "
                        + "* cos((?4-?2)*0.017453292519943295) "
                        + "+ sin(?1*0.017453292519943295) * sin(?3*0.017453292519943295))))",
                functionContributions.getTypeConfiguration()
                        .getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.DOUBLE));
    }
}
