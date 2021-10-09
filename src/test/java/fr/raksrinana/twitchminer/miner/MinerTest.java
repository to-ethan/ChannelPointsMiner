package fr.raksrinana.twitchminer.miner;

import fr.raksrinana.twitchminer.api.gql.GQLApi;
import fr.raksrinana.twitchminer.api.gql.data.GQLResponse;
import fr.raksrinana.twitchminer.api.gql.data.reportmenuitem.ReportMenuItemData;
import fr.raksrinana.twitchminer.api.gql.data.types.User;
import fr.raksrinana.twitchminer.api.kraken.KrakenApi;
import fr.raksrinana.twitchminer.api.kraken.data.follows.Channel;
import fr.raksrinana.twitchminer.api.kraken.data.follows.Follow;
import fr.raksrinana.twitchminer.api.passport.PassportApi;
import fr.raksrinana.twitchminer.api.passport.TwitchLogin;
import fr.raksrinana.twitchminer.api.passport.exceptions.CaptchaSolveRequired;
import fr.raksrinana.twitchminer.api.passport.exceptions.LoginException;
import fr.raksrinana.twitchminer.api.twitch.TwitchApi;
import fr.raksrinana.twitchminer.api.ws.TwitchWebSocketPool;
import fr.raksrinana.twitchminer.api.ws.data.request.topic.Topics;
import fr.raksrinana.twitchminer.config.Configuration;
import fr.raksrinana.twitchminer.config.StreamerConfiguration;
import fr.raksrinana.twitchminer.factory.ApiFactory;
import fr.raksrinana.twitchminer.factory.MinerRunnableFactory;
import fr.raksrinana.twitchminer.factory.StreamerSettingsFactory;
import fr.raksrinana.twitchminer.miner.data.Streamer;
import fr.raksrinana.twitchminer.miner.data.StreamerSettings;
import fr.raksrinana.twitchminer.miner.runnables.UpdateChannelPointsContext;
import fr.raksrinana.twitchminer.miner.runnables.UpdateStreamInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import static fr.raksrinana.twitchminer.api.ws.data.request.topic.TopicName.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinerTest{
	private static final String STREAMER_USERNAME = "streamer-username";
	private static final String STREAMER_ID = "streamer-id";
	private static final String USER_ID = "user-id";
	private static final String ACCESS_TOKEN = "access-token";
	
	@InjectMocks
	private Miner tested;
	
	@Mock
	private Configuration configuration;
	@Mock
	private PassportApi passportApi;
	@Mock
	private TwitchWebSocketPool webSocketPool;
	@Mock
	private StreamerConfiguration streamerConfiguration;
	@Mock
	private StreamerSettingsFactory streamerSettingsFactory;
	@Mock
	private ScheduledExecutorService executor;
	@Mock
	private TwitchLogin twitchLogin;
	@Mock
	private StreamerSettings streamerSettings;
	@Mock
	private TwitchApi twitchApi;
	@Mock
	private GQLApi gqlApi;
	@Mock
	private KrakenApi krakenApi;
	@Mock
	private UpdateStreamInfo updateStreamInfo;
	@Mock
	private UpdateChannelPointsContext updateChannelPointsContext;
	@Mock
	private User user;
	@Mock
	private ReportMenuItemData reportMenuItemData;
	@Mock
	private GQLResponse<ReportMenuItemData> reportMenuItemResponse;
	
	@BeforeEach
	void setUp() throws LoginException, IOException{
		lenient().when(passportApi.login()).thenReturn(twitchLogin);
		lenient().when(streamerSettingsFactory.readStreamerSettings()).thenReturn(streamerSettings);
		lenient().when(streamerSettings.isFollowRaid()).thenReturn(false);
		lenient().when(streamerSettings.isMakePredictions()).thenReturn(false);
		lenient().when(twitchLogin.getUserId()).thenReturn(USER_ID);
		lenient().when(twitchLogin.getAccessToken()).thenReturn(ACCESS_TOKEN);
		
		lenient().when(streamerConfiguration.getUsername()).thenReturn(STREAMER_USERNAME);
		
		lenient().when(reportMenuItemResponse.getData()).thenReturn(reportMenuItemData);
		lenient().when(reportMenuItemData.getUser()).thenReturn(user);
		lenient().when(user.getId()).thenReturn(STREAMER_ID);
	}
	
	@Test
	void setupIsDoneFromConfig() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(gqlApi.reportMenuItem(STREAMER_USERNAME)).thenReturn(Optional.of(reportMenuItemResponse));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void setupIsDoneFromConfigWithPredictions() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			when(streamerSettings.isMakePredictions()).thenReturn(true);
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(gqlApi.reportMenuItem(STREAMER_USERNAME)).thenReturn(Optional.of(reportMenuItemResponse));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(PREDICTIONS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(PREDICTIONS_CHANNEL_V1, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void setupIsDoneFromConfigWithRaid() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			when(streamerSettings.isFollowRaid()).thenReturn(true);
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(gqlApi.reportMenuItem(STREAMER_USERNAME)).thenReturn(Optional.of(reportMenuItemResponse));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(RAID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void setupIsDoneFromConfigWithUnknownUser() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(gqlApi.reportMenuItem(STREAMER_USERNAME)).thenReturn(Optional.empty());
			
			assertDoesNotThrow(() -> tested.start());
			
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).isEmpty();
			
			verify(passportApi).login();
			verify(updateStreamInfo, never()).update(any());
			verify(updateChannelPointsContext, never()).update(any());
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool, never()).listenTopic(Topics.buildFromName(PREDICTIONS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool, never()).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
			verify(webSocketPool, never()).listenTopic(Topics.buildFromName(PREDICTIONS_CHANNEL_V1, STREAMER_ID, ACCESS_TOKEN));
			verify(webSocketPool, never()).listenTopic(Topics.buildFromName(RAID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void setupIsDoneFromFollows() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			apiFactory.when(() -> ApiFactory.getKrakenApi(twitchLogin)).thenReturn(krakenApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			var channel = mock(Channel.class);
			var follow = mock(Follow.class);
			when(follow.getChannel()).thenReturn(channel);
			when(channel.getId()).thenReturn(STREAMER_ID);
			when(channel.getName()).thenReturn(STREAMER_USERNAME);
			
			when(configuration.isLoadFollows()).thenReturn(true);
			when(configuration.getStreamers()).thenReturn(Set.of());
			when(krakenApi.getFollows()).thenReturn(List.of(follow));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void setupIsDoneFromFollowsWithPredictions() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			apiFactory.when(() -> ApiFactory.getKrakenApi(twitchLogin)).thenReturn(krakenApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			var channel = mock(Channel.class);
			var follow = mock(Follow.class);
			when(follow.getChannel()).thenReturn(channel);
			when(channel.getId()).thenReturn(STREAMER_ID);
			when(channel.getName()).thenReturn(STREAMER_USERNAME);
			
			when(configuration.isLoadFollows()).thenReturn(true);
			when(streamerSettings.isMakePredictions()).thenReturn(true);
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(krakenApi.getFollows()).thenReturn(List.of(follow));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(PREDICTIONS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(PREDICTIONS_CHANNEL_V1, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void setupIsDoneFromFollowsWithRaid() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			apiFactory.when(() -> ApiFactory.getKrakenApi(twitchLogin)).thenReturn(krakenApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			var channel = mock(Channel.class);
			var follow = mock(Follow.class);
			when(follow.getChannel()).thenReturn(channel);
			when(channel.getId()).thenReturn(STREAMER_ID);
			when(channel.getName()).thenReturn(STREAMER_USERNAME);
			
			when(configuration.isLoadFollows()).thenReturn(true);
			when(streamerSettings.isFollowRaid()).thenReturn(true);
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(krakenApi.getFollows()).thenReturn(List.of(follow));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(RAID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void duplicateStreamerIsAddedOnce() throws LoginException, IOException{
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			apiFactory.when(() -> ApiFactory.getKrakenApi(twitchLogin)).thenReturn(krakenApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			var channel = mock(Channel.class);
			var follow = mock(Follow.class);
			when(follow.getChannel()).thenReturn(channel);
			when(channel.getName()).thenReturn(STREAMER_USERNAME);
			
			when(configuration.isLoadFollows()).thenReturn(true);
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(gqlApi.reportMenuItem(STREAMER_USERNAME)).thenReturn(Optional.of(reportMenuItemResponse));
			when(krakenApi.getFollows()).thenReturn(List.of(follow));
			
			assertDoesNotThrow(() -> tested.start());
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getTwitchApi()).isEqualTo(twitchApi);
			assertThat(tested.getGqlApi()).isEqualTo(gqlApi);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(passportApi).login();
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void addDuplicateStreamer(){
		try(var apiFactory = Mockito.mockStatic(ApiFactory.class);
				var runnableFactory = Mockito.mockStatic(MinerRunnableFactory.class)){
			apiFactory.when(ApiFactory::getTwitchApi).thenReturn(twitchApi);
			apiFactory.when(() -> ApiFactory.getGqlApi(twitchLogin)).thenReturn(gqlApi);
			
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateStreamInfo(tested)).thenReturn(updateStreamInfo);
			runnableFactory.when(() -> MinerRunnableFactory.getUpdateChannelPointsContext(tested)).thenReturn(updateChannelPointsContext);
			
			when(configuration.getStreamers()).thenReturn(Set.of(streamerConfiguration));
			when(gqlApi.reportMenuItem(STREAMER_USERNAME)).thenReturn(Optional.of(reportMenuItemResponse));
			
			tested.start();
			
			var duplicateStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME.toUpperCase(), streamerSettings);
			
			assertDoesNotThrow(() -> tested.addStreamer(duplicateStreamer));
			
			var expectedStreamer = new Streamer(STREAMER_ID, STREAMER_USERNAME, streamerSettings);
			assertThat(tested.getStreamers()).hasSize(1)
					.first().usingRecursiveComparison().isEqualTo(expectedStreamer);
			
			verify(updateStreamInfo).update(expectedStreamer);
			verify(updateChannelPointsContext).update(expectedStreamer);
			verify(webSocketPool).listenTopic(Topics.buildFromName(COMMUNITY_POINTS_USER_V1, USER_ID, ACCESS_TOKEN));
			verify(webSocketPool).listenTopic(Topics.buildFromName(VIDEO_PLAYBACK_BY_ID, STREAMER_ID, ACCESS_TOKEN));
		}
	}
	
	@Test
	void captchaLogin() throws LoginException, IOException{
		when(passportApi.login()).thenThrow(new CaptchaSolveRequired(500, "For tests"));
		
		assertThrows(IllegalStateException.class, () -> tested.start());
	}
	
	@Test
	void exceptionLogin() throws LoginException, IOException{
		when(passportApi.login()).thenThrow(new RuntimeException("For tests"));
		
		assertThrows(IllegalStateException.class, () -> tested.start());
	}
	
	@Test
	void close(){
		assertDoesNotThrow(() -> tested.close());
		
		verify(executor).shutdown();
		verify(webSocketPool).close();
	}
}