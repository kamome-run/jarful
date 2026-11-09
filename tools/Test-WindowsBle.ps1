# Hardware-independent regression checks; run with Windows PowerShell 5.1.
# powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/Test-WindowsBle.ps1
$ErrorActionPreference = 'Stop'
$helper = Join-Path $PSScriptRoot '../shared/src/desktopMain/resources/jarful-ble.ps1'
$source = [IO.File]::ReadAllText($helper)
$tokens = $null; $errors = $null
$ast = [Management.Automation.Language.Parser]::ParseInput($source, [ref]$tokens, [ref]$errors)
if ($errors.Count -gt 0) { throw ($errors.Message -join '; ') }
# Load the real helper setup/functions, excluding its command dispatcher.
Invoke-Expression $source.Substring(0, $ast.EndBlock.Statements[-1].Extent.StartOffset)

function Assert($condition, [string] $message) {
    if (-not $condition) { throw "FAIL: $message" }
}
function Assert-Throws([scriptblock] $action, [string] $expected) {
    $caught = $null
    try { & $action } catch { $caught = $_.Exception.Message }
    Assert ($null -ne $caught -and $caught.Contains($expected)) "expected $expected; got $caught"
}

# Compile the actual WinRT event bridge without requiring an adapter or printer.
Initialize-BleDiscovery
Assert ($null -ne ('Jarful.BleDiscovery' -as [type])) 'managed discovery bridge compiles on Windows PowerShell'

$number = Get-AddressNumber 'AA:BB:CC:DD:EE:FF'
Assert ((Get-AddressNumber 'aa-bb-cc-dd-ee-ff') -eq $number) 'hyphenated address'
Assert ((Get-AddressNumber 'AABBCCDDEEFF') -eq $number) 'compact address'
Assert ((Format-Address $number) -eq 'AA:BB:CC:DD:EE:FF') 'canonical MAC format'
Assert-Throws { Get-AddressNumber 'COM3' } 'INVALID_BLUETOOTH_ADDRESS'
Assert-Throws { Get-AddressNumber 'GG:BB:CC:DD:EE:FF' } 'INVALID_BLUETOOTH_ADDRESS'

& {
    function Open-BleDevice { return 'cached device' }
    function Find-BleAdvertisements { throw 'should not scan' }
    Assert ((Get-Device 'AA:BB:CC:DD:EE:FF') -eq 'cached device') 'cached connection avoids scanning'
}

& {
    $script:opens = 0; $script:scans = 0
    function Open-BleDevice($num, $addressType) {
        $script:opens++
        Assert ($num -eq $number) 'connect only to the requested device'
        if ($script:opens -eq 2) {
            Assert ($addressType -eq 'Random') 'retry preserves advertised address type'
            return 'discovered device'
        }
        return $null
    }
    function Find-BleAdvertisements($targetAddress) {
        $script:scans++
        Assert ($targetAddress -eq $number) 'targeted discovery'
        return [PSCustomObject]@{ Address = $number; Name = 'Mr.in_M02'; AddressType = 'Random' }
    }
    Assert ((Get-Device 'AA:BB:CC:DD:EE:FF') -eq 'discovered device') 'cache miss is recovered by a live scan'
    Assert ($script:opens -eq 2 -and $script:scans -eq 1) 'one bounded scan and one retry'
}

& {
    function Open-BleDevice { return $null }
    function Find-BleAdvertisements { return [PSCustomObject]@{ Address = 1; Name = 'Other printer'; AddressType = 'Public' } }
    Assert-Throws { Get-Device 'AA:BB:CC:DD:EE:FF' } 'DEVICE_NOT_FOUND: AA:BB:CC:DD:EE:FF'
}

& {
    function Get-KnownBleDevices { throw 'driver enumeration failure' }
    function Find-BleAdvertisements { return [PSCustomObject]@{ Address = $number; Name = 'Mr.in_M02' } }
    $list = @(Get-BleDeviceList)
    Assert ($list.Count -eq 1 -and $list[0] -eq 'AA:BB:CC:DD:EE:FF|Mr.in_M02') 'scan works even if cached enumeration fails'
}

& {
    function Get-KnownBleDevices { return [PSCustomObject]@{ Address = $number; Name = 'Mr.in_M02' } }
    function Find-BleAdvertisements {
        [PSCustomObject]@{ Address = $number; Name = '' }
        [PSCustomObject]@{ Address = 1; Name = "Other|printer`nname" }
    }
    $list = @(Get-BleDeviceList)
    Assert ($list.Count -eq 2) 'deduplicate by canonical address'
    Assert ($list -contains 'AA:BB:CC:DD:EE:FF|Mr.in_M02') 'empty scan-response names preserve known names'
    Assert ($list -contains '00:00:00:00:00:01|Other printer name') 'names cannot corrupt line protocol'
}

$readOnly = [PSCustomObject]@{ Uuid = [Guid]$known[0]; CharacteristicProperties = 16 }
$write = [PSCustomObject]@{ Uuid = [Guid]$known[1]; CharacteristicProperties = 4 }
Assert ((Select-WriteCharacteristic @($readOnly, $write)).Uuid -eq $write.Uuid) 'known UUID must actually be writable'
Assert-Throws { Select-WriteCharacteristic @($readOnly, $write) $readOnly.Uuid.ToString() } 'NO_WRITABLE_CHARACTERISTIC'
Assert ((Select-WriteCharacteristic @($write) $write.Uuid.ToString().ToUpper()).Uuid -eq $write.Uuid) 'explicit UUID is case insensitive'
Assert-Throws { Write-Chunks $write ([byte[]]@(1)) 0 } 'INVALID_CHUNK_SIZE'

Write-Output 'PASS: Windows BLE discovery bridge, connection recovery, list and write-channel regressions'
