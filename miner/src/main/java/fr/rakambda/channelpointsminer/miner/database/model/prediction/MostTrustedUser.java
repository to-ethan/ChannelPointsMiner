package fr.rakambda.channelpointsminer.miner.database.model.prediction;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
public class MostTrustedUser {
    @NotNull
    private final String badge;
    private final double winRate;
    private final int userBetsPlaced;
    private final double averageReturnOnInvestment;
    private final double standardDeviation;
    private final double systemQualityNumber;
}
