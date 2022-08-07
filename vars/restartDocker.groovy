def call(){
    sh """
        systemctl status docker | grep 'Active:'
        sudo /usr/bin/pkill -f docker
        sudo /bin/systemctl restart docker
        docker system prune -a -f
        systemctl status docker | grep 'Active:'
    """
}