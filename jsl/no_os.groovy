// Pipeline
// Pipeline
lock(label: 'adgt_test_harness_boards'){
    @Library('sdgtt-lib@no-os-stage') _ // Not necessary when we turn on global libraries :)
    def hdlBranch = 'release'
    def linuxBranch = 'release'
    def bootPartitionBranch = 'NA'
    def vivado_ver = '2019.1'
    def nebula_local_fs_source_root = 'artifactory.analog.com'
    //def jenkins_job_trigger = "${trigger}"
    def bootfile_source = 'artifactory' // options: sftp, artifactory, http, local
    def harness = getGauntlet(hdlBranch, linuxBranch, bootPartitionBranch)
    //save what triggered the job
    //harness.set_job_trigger(jenkins_job_trigger)

    //udpate repos
    harness.set_env('nebula_repo','https://github.com/sdgtt/nebula.git')
    harness.set_env('nebula_branch','downloader-no-os')
	harness.set_env('nebula_config_branch','no-os-support')
    harness.set_env('libiio_branch','v0.23')
    //harness.set_env('no_os_repo','https://github.com/tagoylo/no-OS.git')
    //harness.set_env('no_os_branch','jtag-multi')
    harness.set_env('vivado_ver', vivado_ver)
    
    //update first the agent with the required deps
	harness.set_required_agent(["sdg-nuc-01"])
    harness.update_agents()
    
    //set other test parameters
    //harness.set_env('docker_image','no-os')
    harness.set_nebula_debug(true)
    harness.set_enable_docker(true)
    harness.set_send_telemetry(true)
    harness.set_enable_resource_queuing(true)
    harness.set_elastic_server('192.168.10.11')

    harness.set_required_hardware(["zynq-zc702-adv7511-ad9361-fmcomms2-3"])
    //harness.set_required_hardware(["adis16480bmlz"])
    harness.set_docker_args(['Vivado'])
    harness.set_nebula_local_fs_source_root("artifactory.analog.com")
    
    def setupsp = {
		stage("Setup libserialport"){
            sh 'sudo apt-get install -y autoconf automake libtool'
            sh 'git clone https://github.com/sigrokproject/libserialport.git'
            dir('libserialport'){
                sh './autogen.sh'
                sh './configure --prefix=/usr/sp'
                sh 'make'
                sh 'make install'
                sh 'cp -r /usr/sp/lib/* /usr/lib/x86_64-linux-gnu/'
                sh 'cp /usr/sp/include/* /usr/include/'
                sh 'date -r /usr/lib/x86_64-linux-gnu/libserialport.so.0'
                }
            }
		}
    
    
    def patch = {
		stage("Clone no-OS and Apply patch"){
            sh 'git clone --recursive -b tfcollins-jtag-multi https://github.com/analogdevicesinc/no-OS.git '
            sh 'git config --global user.email trecia.agoylo@analog.com'
            sh 'git config --global user.name Trecia Agoylo'
            //dir('no-OS'){
            //    if (vivado_ver == '2021.1') {
            //        sh 'wget --no-check-certificate "https://docs.google.com/uc?export=download&id=1UTJ-mCkXcWLDoUv9B8eI1UEY2qlswJsw" -O jtag-2021.1.patch'
            //        sh 'git am jtag-2021.1.patch'
            //        sh 'git log --oneline'
            //    } else {
            //        sh 'wget --no-check-certificate "https://docs.google.com/uc?export=download&id=1jQ3wQMxE35qlhJddoR9HMvQLKcYQW3PK" -O jtag-filtering.patch'
            //        sh 'git am jtag-filtering.patch'
            //        sh 'git log --oneline'
             //   }
            //}
		}
    }
    
    def trial = {
		stage("try"){
            sh 'git clone --recursive -b master https://github.com/analogdevicesinc/no-OS.git '
            dir('no-OS/projects/ad9361'){
                def buildfile = readJSON file: 'builds.json'
                flags = buildfile['xilinx']['iio']['flags']
                println(flags)
            }
		}
    }
    
    def noos = { String board ->
        stage("Build no-OS Project"){
            def pwd = sh(returnStdout: true, script: 'pwd').trim()
            withEnv(['VERBOSE=1', 'BUILD_DIR=' +pwd]){
                def project = nebula('update-config board-config no-os-project --board-name='+board)
                def example = nebula('update-config board-config example --board-name='+board)
                def jtag_cable_id = nebula('update-config jtag-config jtag_cable_id --board-name='+board)
                def files = ['2019.1':'system_top.hdf', '2020.1':'system_top.xsa', '2021.1':'system_top.xsa']
                def file = files[vivado_ver]
                sh 'apt-get install libncurses5-dev libncurses5 -y' //remove once docker image is updated
                nebula('dl.bootfiles --board-name=' + board + ' --source-root="' + nebula_local_fs_source_root + '" --source=' + bootfile_source
                                    +  ' --branch="' + hdlBranch+  '" --filetype="noos"')
                sh 'cp outs/' +file+ ' no-OS/projects/'+ project +'/'
                dir('no-OS'){
                    dir('projects/'+ project){
                        //get flags base on type of demo or test; may create a method to set the demo or type of test\
                        def buildfile = readJSON file: 'builds.json'
                        flags = buildfile['xilinx'][example]['flags']
                        
                        if (vivado_ver == '2020.1' || vivado_ver == '2021.1' ){
                            sh 'ln /usr/bin/make /usr/bin/gmake'
                        }
                        sh 'source /opt/Xilinx/Vivado/' +vivado_ver+ '/settings64.sh && make HARDWARE=' +file+ ' '+flags

                        //call uart log here
                        nebula('uart.get-uart-log --board-name=' + board+' --period=120')

                        retry(3){
                            sleep(2)
                            sh 'source /opt/Xilinx/Vivado/' +vivado_ver+ '/settings64.sh && make run' +' JTAG_CABLE_ID='+jtag_cable_id
                        }
                    }
                }
                //test for different examples
                //check flags
                //if flags has dma example
                //read log
                //assign to variables
                //run 
                switch(example){
                    case ["iio", "iio_fmcomms5"]:
                        //do something
                        sleep(5)
                        def serial = nebula('update-config uart-config address --board-name='+board)
                        sh 'iio_info -u serial:' + serial + ',921600'
                    case ["dma_example", "dma-example"]:
                        //do something
                        
                    case []




                }
            }
        }
    }
    
    def uart = { String board ->
        stage("read uart"){
            sleep(5)
            def serial = nebula('update-config uart-config address --board-name='+board)
            //sh 'script /dev/null'
            //sh 'screen '+serial+ ' 115200'
            sh 'iio_info -u serial:' + serial + ',921600' 
            //sh 'cat '+serial+ ' 115200'
        }
        
    }
	
	harness.add_stage(setupsp)
    harness.add_stage(patch)
    harness.add_stage(noos)
	//harness.add_stage(uart)
    //harness.add_stage(harness.stage_library('noOSTest'))
    //harness.add_stage(harness.stage_library('PyADITests'),'continueWhenFail')
    // // Go go
    harness.run_stages()
}