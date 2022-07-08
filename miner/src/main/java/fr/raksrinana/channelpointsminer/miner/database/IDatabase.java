package fr.raksrinana.channelpointsminer.miner.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public interface IDatabase extends AutoCloseable{
	void initDatabase() throws SQLException;
	
	void createChannel(@NotNull String channelId, @NotNull String username) throws SQLException;
	
	void updateChannelStatusTime(@NotNull String channelId, @NotNull Instant instant) throws SQLException;
	
	void addBalance(@NotNull String channelId, int balance, @Nullable String reason, @NotNull Instant instant) throws SQLException;
	
	void addPrediction(@NotNull String channelId, @NotNull String eventId, @NotNull String type, @NotNull String description, @NotNull Instant instant) throws SQLException;
	
	void addUserPrediction(@NotNull String username, @NotNull String channelName, @NotNull String badge) throws SQLException;
	
	void cancelPrediction(@NotNull String eventId, @NotNull String channelId, @NotNull String title, @NotNull Instant eventCreated,
			@NotNull Instant eventEnded) throws SQLException;
	
	void resolvePrediction(@NotNull String eventId, @NotNull String channelId, @NotNull String title, @NotNull Instant eventCreated,
			@NotNull Instant eventEnded, @NotNull String outcome, @NotNull String badge) throws SQLException;
	
	void deleteUnresolvedUserPredictions() throws SQLException;
	
	void deleteUnresolvedUserPredictionsForChannel(@NotNull String channelId) throws SQLException;
	
	@Override
	void close();
	
	@NotNull
	Connection getConnection() throws SQLException;
}
