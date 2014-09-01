set :stage, :prod

set :ssh_options, {
  auth_methods: ["password"],
  user: 'smx',
  password: 'Kon6QuaytUc?',
  port: 8101,
  paranoid: false
}

role :karaf, %w{esb-a.colo.elex.be esb-b.colo.elex.be}
