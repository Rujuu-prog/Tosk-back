import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GenerateJwks {
    public static void main(String[] args) throws Exception {
        System.out.println("🔐 JWT用JWKS鍵ペアを生成します...");
        
        // kid生成（tosk-YYYYMMDD-HHmm形式）
        String kid = "tosk-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        System.out.println("📝 Key ID: " + kid);
        
        // RSA鍵ペア生成
        System.out.println("📝 RSA鍵ペア生成中...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        
        // JWK作成
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(kid)
            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .build();
        
        // Private JWKS (秘密鍵含む)
        JWKSet privateJwkSet = new JWKSet(rsaKey);
        
        // Public JWKS (公開鍵のみ)
        JWKSet publicJwkSet = new JWKSet(rsaKey.toPublicJWK());
        
        // keysディレクトリ作成
        Path keysDir = Paths.get("keys");
        Files.createDirectories(keysDir);
        
        // ファイル出力
        Files.writeString(keysDir.resolve("private-jwks.json"), privateJwkSet.toString(true));
        Files.writeString(keysDir.resolve("public-jwks.json"), publicJwkSet.toString(true));
        
        // Base64エンコード
        String privateJwksB64 = Base64.getEncoder().encodeToString(
            privateJwkSet.toString().getBytes(StandardCharsets.UTF_8)
        );
        
        System.out.println("");
        System.out.println("✅ 鍵ペア生成完了！");
        System.out.println("");
        System.out.println("生成されたファイル:");
        System.out.println("  Private JWKS: keys/private-jwks.json");
        System.out.println("  Public JWKS: keys/public-jwks.json");
        System.out.println("  Key ID: " + kid);
        System.out.println("");
        System.out.println("⚠️  重要な注意事項:");
        System.out.println("  - Private JWKSは絶対に外部に漏らさないでください");
        System.out.println("");
        System.out.println("📋 環境変数設定:");
        System.out.println("");
        System.out.println("# .env ファイルに以下を追加:");
        System.out.println("APP_JWT_PRIVATE_JWKS_B64=\"" + privateJwksB64 + "\"");
    }
}