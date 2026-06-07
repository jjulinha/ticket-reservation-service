package com.unisul.seatreservation.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Ensina o MyBatis a converter UUID (Java) <-> BINARY(16) (MySQL).
 * Sem isto, cada query precisaria converter o ID na mao.
 */
@MappedTypes(UUID.class)
public class UuidBinaryTypeHandler extends BaseTypeHandler<UUID> {

    // Java -> Banco: transforma o UUID em 16 bytes antes de gravar
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setBytes(i, uuidToBytes(parameter));
    }

    // Banco -> Java: le os 16 bytes e remonta o UUID (busca por nome de coluna)
    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return bytesToUuid(rs.getBytes(columnName));
    }

    // Banco -> Java: mesma coisa, mas buscando por posicao da coluna
    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return bytesToUuid(rs.getBytes(columnIndex));
    }

    // Banco -> Java: usado em stored procedures
    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return bytesToUuid(cs.getBytes(columnIndex));
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private UUID bytesToUuid(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long high = buffer.getLong();
        long low = buffer.getLong();
        return new UUID(high, low);
    }
}