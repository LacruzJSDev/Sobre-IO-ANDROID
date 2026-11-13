import GoogleSignIn
import Shared
import UIKit

/// Google sign-in through the GoogleSignIn SDK, called from the shared Kotlin code (`IosGoogleSignInProvider`).
final class GoogleSignInProvider: IosGoogleSignInProvider {
    func signIn(hashedNonce: String, onResult: @escaping (String?, KotlinBoolean) -> Void) {
        DispatchQueue.main.async {
            guard let presenting = Self.topViewController() else {
                onResult(nil, KotlinBoolean(bool: false))
                return
            }
            GIDSignIn.sharedInstance.signIn(
                withPresenting: presenting,
                hint: nil,
                additionalScopes: nil,
                nonce: hashedNonce
            ) { result, error in
                if let token = result?.user.idToken?.tokenString {
                    onResult(token, KotlinBoolean(bool: false))
                    return
                }
                let cancelled = (error as NSError?).map {
                    $0.domain == kGIDSignInErrorDomain && $0.code == GIDSignInError.canceled.rawValue
                } ?? false
                onResult(nil, KotlinBoolean(bool: cancelled))
            }
        }
    }

    /// The view controller currently on top of the key window, where Google shows its sheet.
    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
