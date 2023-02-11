package fr.rakambda.channelpointsminer.miner.database;

import com.zaxxer.hikari.HikariDataSource;
import fr.rakambda.channelpointsminer.miner.database.converter.Converters;
import fr.rakambda.channelpointsminer.miner.factory.TimeFactory;
import org.jetbrains.annotations.NotNull;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SQLiteDatabase extends BaseDatabase {
	public SQLiteDatabase(HikariDataSource dataSource){
		super(dataSource);
	}
	
	@Override
	public void initDatabase() throws SQLException{
		applyFlyway("db/migrations/sqlite");
	}
	
	@Override
	public void createChannel(@NotNull String channelId, @NotNull String username) throws SQLException{
		try(var conn = getConnection();
				var statement = conn.prepareStatement("""
						INSERT OR IGNORE INTO `Channel`(`ID`, `Username`, `LastStatusChange`)
						VALUES(?, ?, ?);"""
				)){
			
			statement.setString(1, channelId);
			statement.setString(2, username);
			statement.setTimestamp(3, Timestamp.from(TimeFactory.now()));
			
			statement.executeUpdate();
		}
	}
	
	@Override
	protected void addUserPrediction(@NotNull String channelId, int userId, @NotNull String badge) throws SQLException{
		try(var conn = getConnection();
				var predictionStatement = conn.prepareStatement("""
						INSERT OR IGNORE INTO `UserPrediction`(`ChannelID`, `UserID`, `Badge`)
						VALUES(?,?,?)"""
				)){
			
			predictionStatement.setString(1, channelId);
			predictionStatement.setInt(2, userId);
			predictionStatement.setString(3, badge);
			
			predictionStatement.executeUpdate();
		}
	}
	
	@Override
	protected void resolveUserPredictions(double returnRatioForWin, @NotNull String channelId, @NotNull String badge) throws SQLException{
		try(var conn = getConnection();
				var getOpenPredictionStmt = conn.prepareStatement("""
						SELECT * FROM `UserPrediction` WHERE `ChannelID`=?""");
				var updatePredictionUserStmt = conn.prepareStatement("""
						WITH wi AS (SELECT ? AS n)
						UPDATE `PredictionUser`
						SET
						`PredictionCnt`=`PredictionCnt`+1,
						`WinCnt`=`WinCnt`+wi.n,
						`WinRate`=CAST((`WinCnt`+wi.n) AS REAL)/(`PredictionCnt`+1),
						`ReturnOnInvestment`=`ReturnOnInvestment`+?
						FROM wi
						WHERE `ID`=? AND `ChannelID`=?""");
				var updateAverageAndSumOfReturnUserStmt = conn.prepareStatement("""
						UPDATE `PredictionUser`
						SET
						`AverageReturnOnInvestment`=`ReturnOnInvestment`/`PredictionCnt`,
						`SumOfReturnOnInvestmentSquared`=`SumOfReturnOnInvestmentSquared`+?*?
						WHERE `ID`=? AND `ChannelID`=?""");
                // Standard Deviation of a Population: https://math.stackexchange.com/a/3941373
                var updateStandardDeviationUserStmt = conn.prepareStatement("""
						UPDATE `PredictionUser`
						SET
						`StandardDeviation`=SQRT(`SumOfReturnOnInvestmentSquared`/`PredictionCnt`-(`ReturnOnInvestment`/`PredictionCnt`)*(`ReturnOnInvestment`/`PredictionCnt`))
						WHERE `ID`=? AND `ChannelID`=? AND `PredictionCnt`>1""");
                // SQN N=100 limit
                var updateSystemQualityNumberUserStmt = conn.prepareStatement("""
						UPDATE `PredictionUser`
						SET
						`SystemQualityNumber`=SQRT(MIN(100, `PredictionCnt`))*`AverageReturnOnInvestment`/`StandardDeviation`
						WHERE `ID`=? AND `ChannelID`=? AND `StandardDeviation`>0""");
		){
            double returnOnInvestment = returnRatioForWin - 1;
            
            getOpenPredictionStmt.setString(1, channelId);
            try(var result = getOpenPredictionStmt.executeQuery()){
                while(result.next()){
                    var userPrediction = Converters.convertUserPrediction(result);
                    if(badge.equals(userPrediction.getBadge())){
                        updatePredictionUserStmt.setInt(1, 1);
                        updatePredictionUserStmt.setDouble(2, returnOnInvestment);
                    }
                    else{
                        updatePredictionUserStmt.setInt(1, 0);
                        updatePredictionUserStmt.setDouble(2, -1);
                    }
                    updatePredictionUserStmt.setInt(3, userPrediction.getUserId());
                    updatePredictionUserStmt.setString(4, userPrediction.getChannelId());
                    updatePredictionUserStmt.addBatch();
                }
                updatePredictionUserStmt.executeBatch();
            }
            
            try(var result = getOpenPredictionStmt.executeQuery()){
                while(result.next()){
                    var userPrediction = Converters.convertUserPrediction(result);
                    if(badge.equals(userPrediction.getBadge())){
                        updateAverageAndSumOfReturnUserStmt.setDouble(1, returnOnInvestment);
                        updateAverageAndSumOfReturnUserStmt.setDouble(2, returnOnInvestment);
                    }
                    else{
                        updateAverageAndSumOfReturnUserStmt.setDouble(1, -1);
                        updateAverageAndSumOfReturnUserStmt.setDouble(2, -1);
                    }
                    updateAverageAndSumOfReturnUserStmt.setInt(3, userPrediction.getUserId());
                    updateAverageAndSumOfReturnUserStmt.setString(4, userPrediction.getChannelId());
                    updateAverageAndSumOfReturnUserStmt.addBatch();
                }
                updateAverageAndSumOfReturnUserStmt.executeBatch();
            }
            executeStatements(getOpenPredictionStmt, updateStandardDeviationUserStmt);
            executeStatements(getOpenPredictionStmt, updateSystemQualityNumberUserStmt);
        }
	}
    
    private void executeStatements(PreparedStatement fetchStatement, PreparedStatement updateStatement) throws SQLException{
        try(var result = fetchStatement.executeQuery()){
            while(result.next()){
                var userPrediction = Converters.convertUserPrediction(result);
                updateStatement.setInt(1, userPrediction.getUserId());
                updateStatement.setString(2, userPrediction.getChannelId());
                updateStatement.addBatch();
            }
            updateStatement.executeBatch();
        }
    }
}
