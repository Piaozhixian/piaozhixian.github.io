import SwiftUI
import SpriteKit

struct ContentView: View {
    private let scene: GameScene = {
        let scene = GameScene(size: CGSize(width: 350, height: 640))
        scene.scaleMode = .aspectFit
        return scene
    }()

    var body: some View {
        GeometryReader { proxy in
            SpriteView(scene: scene)
                .frame(width: proxy.size.width, height: proxy.size.height)
                .ignoresSafeArea()
        }
        .background(Color(red: 0.96, green: 0.93, blue: 0.85))
    }
}

#Preview {
    ContentView()
}
