$path = 'src/main/java/org/ether/society/engines/compiler/DynamicEngineCompiler.java'
$c = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
$c = $c.Replace('import org.ether.society.engines.ProceduralEngineRegistry;', "import org.ether.society.core.engine.SimulationEngineModule;`r`nimport org.ether.society.core.engine.SimulationEngineRegistry;")
$c = $c.Replace('ProceduralEnginePlugin.class.isAssignableFrom(loadedClass)', 'SimulationEngineModule.class.isAssignableFrom(loadedClass)')
$c = $c.Replace('"Class must implement org.ether.society.engines.ProceduralEnginePlugin"', '"Class must implement org.ether.society.core.engine.SimulationEngineModule"')
$c = $c.Replace('ProceduralEnginePlugin pluginInstance = (ProceduralEnginePlugin) loadedClass.getDeclaredConstructor().newInstance();', 'SimulationEngineModule pluginInstance = (SimulationEngineModule) loadedClass.getDeclaredConstructor().newInstance();')
$c = $c.Replace('ProceduralEngineRegistry.registerPlugin(simpleClassName, pluginInstance);', 'SimulationEngineRegistry.getInstance().register(pluginInstance);')
$c = $c.Replace('ProceduralEngineRegistry', 'SimulationEngineRegistry')
$c = $c.Replace('implements the ProceduralEnginePlugin interface', 'extends AbstractSimulationEngine')
[System.IO.File]::WriteAllText($path, $c, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "Successfully updated DynamicEngineCompiler"
