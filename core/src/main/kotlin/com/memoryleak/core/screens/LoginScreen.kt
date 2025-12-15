package com.memoryleak.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.memoryleak.core.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginScreen(
    private val batch: SpriteBatch,
    private val network: NetworkClient,
    private val onLoginSuccess: (String) -> Unit
) {
    val stage: Stage
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // UI Elements
    private val usernameField: TextField
    private val passwordField: TextField
    private val statusLabel: Label
    
    init {
        val viewport = ExtendViewport(800f, 600f)
        stage = Stage(viewport, batch)
        
        // --- 1. Programmatic Styles (No Skin File Needed) ---
        // Create a 1x1 white texture for backgrounds
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        val whiteTexture = Texture(pixmap)
        pixmap.dispose()
        
        val whiteDrawable = TextureRegionDrawable(whiteTexture)
        
        val font = BitmapFont() // Default Arial font
        // Enable linear filter for sharper text
        font.region.texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        
        // TextField Style
        val tfStyle = TextField.TextFieldStyle()
        tfStyle.font = font
        tfStyle.fontColor = Color.WHITE
        tfStyle.cursor = whiteDrawable.tint(Color.CYAN)
        tfStyle.selection = whiteDrawable.tint(Color.BLUE)
        tfStyle.background = whiteDrawable.tint(Color(0.2f, 0.2f, 0.2f, 1f))
        
        // Button Style
        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.fontColor = Color.BLACK
        btnStyle.up = whiteDrawable.tint(Color.LIGHT_GRAY)
        btnStyle.down = whiteDrawable.tint(Color.GRAY)
        btnStyle.over = whiteDrawable.tint(Color.WHITE)
        
        // Label Style
        val lblStyle = Label.LabelStyle(font, Color.WHITE)
        
        // --- 2. Layout ---
        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)
        
        root.add(Label("MEMORY LEAK - AUTH", lblStyle)).padBottom(40f).row()
        
        root.add(Label("Username:", lblStyle)).left().row()
        usernameField = TextField("", tfStyle)
        root.add(usernameField).width(300f).padBottom(10f).row()
        
        root.add(Label("Password:", lblStyle)).left().row()
        passwordField = TextField("", tfStyle)
        passwordField.isPasswordMode = true
        passwordField.setPasswordCharacter('*')
        root.add(passwordField).width(300f).padBottom(30f).row()
        
        val buttonsTable = Table()
        val loginBtn = TextButton("LOGIN", btnStyle)
        val registerBtn = TextButton("REGISTER", btnStyle)
        
        buttonsTable.add(loginBtn).width(140f).padRight(20f)
        buttonsTable.add(registerBtn).width(140f)
        root.add(buttonsTable).row()
        
        statusLabel = Label("Ready", lblStyle)
        statusLabel.color = Color.YELLOW
        root.add(statusLabel).padTop(20f)
        
        // --- 3. Logic ---
        loginBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                performAuth(isRegister = false)
            }
        })
        
        registerBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                performAuth(isRegister = true)
            }
        })
    }
    
    private fun performAuth(isRegister: Boolean) {
        val user = usernameField.text
        val pass = passwordField.text
        
        if (user.isBlank() || pass.isBlank()) {
            statusLabel.setText("Fill all fields!")
            return
        }
        
        statusLabel.setText("Connecting...")
        
        // Coroutine for network call
        scope.launch(Dispatchers.IO) {
            val token = if (isRegister) {
                network.register(user, pass)
            } else {
                network.login(user, pass)
            }
            
            // Switch to GDX main thread safely
            Gdx.app.postRunnable {
                if (token != null) {
                    statusLabel.setText("Success! Loading...")
                    onLoginSuccess(token)
                } else {
                    statusLabel.setText(if (isRegister) "Reg failed (User exists?)" else "Login failed")
                }
            }
        }
    }
    
    fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }
    
    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }
    
    fun dispose() {
        stage.dispose()
    }
}
