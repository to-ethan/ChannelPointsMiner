package fr.raksrinana.channelpointsminer.miner.api.gql.data.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.raksrinana.channelpointsminer.miner.util.json.UnknownDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@JsonTypeName("ClaimCommunityMomentPayload")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString
public class ClaimCommunityMomentPayload extends GQLType{
	@JsonProperty("moment")
	@Nullable
	private CommunityMoment moment;
	@JsonProperty("error")
	@JsonDeserialize(using = UnknownDeserializer.class)
	@Nullable
	private Object error;
}
