package fr.raksrinana.twitchminer.api.gql.data.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import org.jetbrains.annotations.Nullable;

@JsonTypeName("ClaimCommunityPointsPayload")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString
public class ClaimCommunityPointsPayload extends GQLType{
	@JsonProperty("claim")
	@Nullable
	private Object claim;
	@JsonProperty("currentPoints")
	@Nullable
	private Object currentPoints;
	@JsonProperty("error")
	@Nullable
	private ClaimCommunityPointsError error;
}
