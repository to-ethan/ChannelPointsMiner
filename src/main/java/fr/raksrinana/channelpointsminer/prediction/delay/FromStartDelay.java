package fr.raksrinana.channelpointsminer.prediction.delay;

import com.fasterxml.jackson.annotation.JsonTypeName;
import fr.raksrinana.channelpointsminer.api.ws.data.message.subtype.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import java.time.ZonedDateTime;

@JsonTypeName("fromStart")
@Getter
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Log4j2
public class FromStartDelay implements IDelayCalculator{
	private int seconds;
	
	@Override
	@NotNull
	public ZonedDateTime calculate(@NotNull Event event){
		return event.getCreatedAt().plusSeconds(seconds);
	}
}
