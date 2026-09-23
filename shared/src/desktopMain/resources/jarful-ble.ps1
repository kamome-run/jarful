# Jarful BLE helper for Windows (WinRT via Windows PowerShell 5.1). No native code, no extra installs.
#   list                              -> prints "<address>|<name>" per line for known/paired BLE devices
#   write <addr> <charUuid|auto> <file> <chunk> -> writes the file's bytes to the characteristic in chunks
#   services <addr>                   -> prints services/characteristics (diagnostics)
param(
    [Parameter(Position = 0)] [string] $Mode,
    [Parameter(Position = 1)] [string] $Address = "",
    [Parameter(Position = 2)] [string] $CharUuid = "auto",
    [Parameter(Position = 3)] [string] $DataFile = "",
    [Parameter(Position = 4)] [int] $Chunk = 20
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime] | Out-Null
[Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService,Windows.Devices.Bluetooth,ContentType=WindowsRuntime] | Out-Null
[Windows.Devices.Enumeration.DeviceInformation,Windows.Devices.Enumeration,ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.Streams.DataWriter,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]
function Await($WinRtTask, $ResultType) {
    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
    $netTask = $asTask.Invoke($null, @($WinRtTask))
    if (-not $netTask.Wait(20000)) { throw "TIMEOUT" }
    return $netTask.Result
}

$known = @(
    "0000ff02-0000-1000-8000-00805f9b34fb", "0000ae01-0000-1000-8000-00805f9b34fb", "00002af1-0000-1000-8000-00805f9b34fb",
    "49535343-8841-43f4-a8d4-ecbe34729bb3", "bef8d6c9-9c21-4c9e-b632-bd58c1009f9f", "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
)

function Get-Device([string] $addr) {
    $hex = $addr -replace '[:\-]', ''
    $num = [Convert]::ToUInt64($hex, 16)
    $dev = Await ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($num)) ([Windows.Devices.Bluetooth.BluetoothLEDevice])
    if ($null -eq $dev) { throw "DEVICE_NOT_FOUND (turn the printer on; pair it once in Windows Bluetooth settings)" }
    return $dev
}

function Get-Characteristics($dev) {
    $svcRes = Await ($dev.GetGattServicesAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult])
    if ($svcRes.Status -ne 'Success') { throw "GATT_SERVICES_$($svcRes.Status)" }
    $all = @()
    foreach ($svc in $svcRes.Services) {
        $chRes = Await ($svc.GetCharacteristicsAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult])
        if ($chRes.Status -eq 'Success') { foreach ($c in $chRes.Characteristics) { $all += $c } }
    }
    return $all
}

switch ($Mode) {
    "list" {
        foreach ($paired in @($true, $false)) {
            $sel = [Windows.Devices.Bluetooth.BluetoothLEDevice]::GetDeviceSelectorFromPairingState($paired)
            $infos = Await ([Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($sel)) ([Windows.Devices.Enumeration.DeviceInformationCollection])
            foreach ($i in $infos) {
                # Id looks like BluetoothLE#BluetoothLE<host mac>-<device mac>
                if ($i.Id -match '-([0-9a-f]{2}:){5}[0-9a-f]{2}$') {
                    $mac = $Matches[0].Substring(1).ToUpper()
                    $name = if ([string]::IsNullOrWhiteSpace($i.Name)) { $mac } else { $i.Name }
                    Write-Output "$mac|$name"
                }
            }
        }
    }
    "services" {
        $dev = Get-Device $Address
        Write-Output "device: $($dev.Name) [$Address] connection=$($dev.ConnectionStatus)"
        foreach ($c in (Get-Characteristics $dev)) {
            $mark = if ($known -contains $c.Uuid.ToString().ToLower()) { " <- known printer characteristic" } else { "" }
            Write-Output ("  char {0} [{1}]{2}" -f $c.Uuid, $c.CharacteristicProperties, $mark)
        }
    }
    "write" {
        $dev = Get-Device $Address
        $chars = Get-Characteristics $dev
        $target = $null
        if ($CharUuid -ne "auto") {
            $target = $chars | Where-Object { $_.Uuid.ToString().ToLower() -eq $CharUuid.ToLower() } | Select-Object -First 1
        } else {
            foreach ($k in $known) { if ($null -eq $target) { $target = $chars | Where-Object { $_.Uuid.ToString().ToLower() -eq $k } | Select-Object -First 1 } }
            if ($null -eq $target) { $target = $chars | Where-Object { ($_.CharacteristicProperties -band 12) -ne 0 } | Select-Object -First 1 }
        }
        if ($null -eq $target) { throw "NO_WRITABLE_CHARACTERISTIC" }
        $noResp = ($target.CharacteristicProperties -band [Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicProperties]::WriteWithoutResponse) -ne 0
        $option = if ($noResp) { [Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteOption]::WriteWithoutResponse } else { [Windows.Devices.Bluetooth.GenericAttributeProfile.GattWriteOption]::WriteWithResponse }
        $bytes = [System.IO.File]::ReadAllBytes($DataFile)
        $i = 0
        while ($i -lt $bytes.Length) {
            $len = [Math]::Min($Chunk, $bytes.Length - $i)
            $writer = New-Object Windows.Storage.Streams.DataWriter
            $slice = $bytes[$i..($i + $len - 1)]
            $writer.WriteBytes([byte[]]$slice)
            $status = Await ($target.WriteValueAsync($writer.DetachBuffer(), $option)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus])
            if ($status -ne 'Success') { throw "WRITE_FAILED_$status" }
            $i += $len
            if ($noResp) { Start-Sleep -Milliseconds 12 }
        }
        Start-Sleep -Milliseconds 400
        Write-Output "OK $($bytes.Length)"
    }
    default { throw "USAGE: list | services <addr> | write <addr> <char|auto> <file> <chunk>" }
}
