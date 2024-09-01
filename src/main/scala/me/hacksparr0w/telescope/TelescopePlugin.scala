package me.hacksparr0w.telescope

import com.google.inject.Inject

import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.core.metrics.Info
import io.prometheus.metrics.exporter.httpserver.HTTPServer
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics

import java.util.Optional
import java.time.Duration

import org.apache.logging.log4j.Logger
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

import org.spongepowered.api.Game
import org.spongepowered.api.MinecraftVersion
import org.spongepowered.api.Server
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.api.scheduler.Task

import org.spongepowered.plugin.builtin.jvm.Plugin
import org.spongepowered.plugin.PluginContainer

import scala.collection.JavaConverters._

@Plugin("telescope")
class TelescopePlugin @Inject() (
  private val logger: Logger,
  private val game: Game,
  private val plugin: PluginContainer
):

  val spongeRuntimeInfo: Info = 
    Info.builder
      .name("sponge_runtime_info")
      .help("Sponge runtime info")
      .labelNames(
        "platform_name",
        "platform_version",
        "api_name",
        "api_version",
        "minecraft_version",
        "minecraft_protocol_version"
      )
      .register

  val spongeOnlinePlayers: Gauge = 
    Gauge.builder
      .name("sponge_online_players")
      .help("Number of players currently online")
      .register
    
  val spongeTargetTps: Gauge = 
    Gauge.builder
      .name("sponge_target_tps")
      .help("Target ticks per second")
      .register

  val spongeCurrentTps: Gauge = 
    Gauge.builder
      .name("sponge_current_tps")
      .help("Current ticks per second")
      .register
  
  val spongeLoadedWorlds: Gauge = 
    Gauge.builder
      .name("sponge_loaded_worlds")
      .help("Number of loaded worlds")
      .register
  
  val spongeLoadedChunks: Gauge =
    Gauge.builder
      .name("sponge_loaded_chunks")
      .help("Number of loaded chunks")
      .register

  @Listener
  def onServerStart(event: StartedEngineEvent[Server]) =
    JvmMetrics.builder.register

    val platformData = game.platform.asMap.asScala

    spongeRuntimeInfo.setLabelValues(
      platformData("PlatformName").asInstanceOf[Optional[String]].get,
      platformData("PlatformVersion").asInstanceOf[DefaultArtifactVersion]
        .toString,
      platformData("APIName").asInstanceOf[Optional[String]].get,
      platformData("APIVersion").asInstanceOf[DefaultArtifactVersion]
        .toString,
      platformData("MinecraftVersion").asInstanceOf[MinecraftVersion].name,
      platformData("MinecraftVersion").asInstanceOf[MinecraftVersion]
        .protocolVersion.toString
    )

    spongeTargetTps.set(game.server.targetTicksPerSecond)

    game.asyncScheduler.submit(
      Task.builder
        .plugin(plugin)
        .execute(() => spongeCurrentTps.set(game.server.ticksPerSecond))
        .interval(Duration.ofMillis(500))
        .build
    )

    game.asyncScheduler.submit(
      Task.builder
        .plugin(plugin)
        .execute(() => spongeLoadedWorlds.set(game.server.worldManager.worlds.size))
        .interval(Duration.ofSeconds(5))
        .build
    )

    game.asyncScheduler.submit(
      Task.builder
        .plugin(plugin)
        .execute(() => spongeLoadedChunks.set(
          game.server.worldManager.worlds.asScala
            .map(_.loadedChunks.asScala.size)
            .sum
        ))
        .interval(Duration.ofSeconds(5))
        .build
    )

    HTTPServer.builder
      .port(8080)
      .executorService(game.asyncScheduler.executor(plugin))
      .buildAndStart

    logger.info("Plugin started successfully")

  @Listener
  def onPlayerJoin(event: ServerSideConnectionEvent.Join) =
    spongeOnlinePlayers.set(game.server.onlinePlayers.size)

  @Listener
  def onPlayerDisconnect(event: ServerSideConnectionEvent.Disconnect) =
    spongeOnlinePlayers.set(game.server.onlinePlayers.size)
