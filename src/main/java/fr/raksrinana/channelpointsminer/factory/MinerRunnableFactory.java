package fr.raksrinana.channelpointsminer.factory;

import fr.raksrinana.channelpointsminer.miner.IMiner;
import fr.raksrinana.channelpointsminer.runnable.SendMinutesWatched;
import fr.raksrinana.channelpointsminer.runnable.StreamerConfigurationReload;
import fr.raksrinana.channelpointsminer.runnable.SyncInventory;
import fr.raksrinana.channelpointsminer.runnable.UpdateStreamInfo;
import fr.raksrinana.channelpointsminer.runnable.WebSocketPing;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class MinerRunnableFactory{
	@NotNull
	public static UpdateStreamInfo createUpdateStreamInfo(@NotNull IMiner miner){
		return new UpdateStreamInfo(miner);
	}
	
	@NotNull
	public static SendMinutesWatched createSendMinutesWatched(@NotNull IMiner miner){
		return new SendMinutesWatched(miner);
	}
	
	@NotNull
	public static WebSocketPing createWebSocketPing(@NotNull IMiner miner){
		return new WebSocketPing(miner);
	}
	
	@NotNull
	public static SyncInventory createSyncInventory(@NotNull IMiner miner){
		return new SyncInventory(miner);
	}
	
	@NotNull
	public static StreamerConfigurationReload createStreamerConfigurationReload(@NotNull IMiner miner, @NotNull StreamerSettingsFactory streamerSettingsFactory, boolean loadFollows){
		return new StreamerConfigurationReload(miner, streamerSettingsFactory, loadFollows);
	}
}
