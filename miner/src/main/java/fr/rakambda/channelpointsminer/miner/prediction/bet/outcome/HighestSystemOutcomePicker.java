package fr.rakambda.channelpointsminer.miner.prediction.bet.outcome;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import fr.rakambda.channelpointsminer.miner.api.ws.data.message.subtype.Outcome;
import fr.rakambda.channelpointsminer.miner.database.IDatabase;
import fr.rakambda.channelpointsminer.miner.database.NoOpDatabase;
import fr.rakambda.channelpointsminer.miner.database.model.prediction.MostTrustedUser;
import fr.rakambda.channelpointsminer.miner.handler.data.BettingPrediction;
import fr.rakambda.channelpointsminer.miner.prediction.bet.exception.BetPlacementException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@JsonTypeName("highestSystem")
@Getter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
@JsonClassDescription("Choose the outcome that's backed by the highest System Quality Number of a user. Requires analytics to be enabled and recordChatsPredictions to be activated.")
public class HighestSystemOutcomePicker implements IOutcomePicker{
	
	@JsonProperty("minTotalBetsPlacedByUser")
	@JsonPropertyDescription("Only user with at least this number of bets are considered in the calculation. Default: 5")
	@Builder.Default
	private int minTotalBetsPlacedByUser = 5;
    @JsonProperty("minSystemQualityNumber")
    @JsonPropertyDescription("Need at least x system quality number to bet on this prediction. Default: 2")
    @Builder.Default
    private double minSystemQualityNumber = 2;
	
	@Override
	@NotNull
	public Outcome chooseOutcome(@NotNull BettingPrediction bettingPrediction, @NotNull IDatabase database) throws BetPlacementException{
		
		try{
			if(database instanceof NoOpDatabase){
				throw new BetPlacementException("A database needs to be configured for this outcome picker to work");
			}
			
			var outcomes = bettingPrediction.getEvent().getOutcomes();
			var title = bettingPrediction.getEvent().getTitle();
			
			var mostTrustedUsers = database.getHighestPredictionUsersForChannel(bettingPrediction.getEvent().getChannelId(), minTotalBetsPlacedByUser, minSystemQualityNumber);
			
			var mostTrusted = mostTrustedUsers.stream()
					.max(Comparator.comparingDouble(MostTrustedUser::getSystemQualityNumber))
					.orElseThrow(() -> new BetPlacementException("No outcome statistics found. Maybe not enough data gathered yet."));
			
			for(var mostTrustedUser : mostTrustedUsers){
				log.info("Trusted user stats for '{}': {}", mostTrustedUser.getBadge(), mostTrustedUser.toString());
			}
			
			var chosenOutcome = outcomes.stream()
					.filter(o -> o.getBadge().getVersion().equalsIgnoreCase(mostTrusted.getBadge()))
					.findAny()
					.orElseThrow(() -> new BetPlacementException("Outcome badge not found: %s".formatted(mostTrusted.getBadge())));
			
			log.info("Prediction: '{}'. Most trusted outcome (highest system quality number of all bettors): Title: '{}', Badge: {}.",
					title, chosenOutcome.getTitle(), chosenOutcome.getBadge().getVersion());
			
			return chosenOutcome;
		}
		catch(BetPlacementException e){
			throw e;
		}
		catch(Exception e){
			throw new BetPlacementException("Bet placement failed", e);
		}
	}
}
