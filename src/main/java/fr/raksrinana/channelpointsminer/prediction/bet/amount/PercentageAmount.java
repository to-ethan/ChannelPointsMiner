package fr.raksrinana.channelpointsminer.prediction.bet.amount;

import com.fasterxml.jackson.annotation.JsonTypeName;
import fr.raksrinana.channelpointsminer.api.ws.data.message.subtype.Outcome;
import fr.raksrinana.channelpointsminer.handler.data.BettingPrediction;
import fr.raksrinana.channelpointsminer.prediction.bet.BetPlacementException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@JsonTypeName("percentage")
@Getter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class PercentageAmount implements IAmountCalculator{
	private float percentage;
	private int max;
	
	@Override
	public int calculateAmount(@NotNull BettingPrediction bettingPrediction, @NotNull Outcome outcome) throws BetPlacementException{
		var currentPoints = bettingPrediction.getStreamer().getChannelPoints().orElseThrow(() -> new BetPlacementException("Failed to get current owned channel points"));
		return (int) Math.min(currentPoints * percentage, max);
	}
}
